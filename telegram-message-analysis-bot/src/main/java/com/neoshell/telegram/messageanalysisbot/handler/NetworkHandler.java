package com.neoshell.telegram.messageanalysisbot.handler;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;
import com.neoshell.telegram.messageanalysisbot.visualization.GraphVisualizationInterface;

public class NetworkHandler extends Handler {

  private static final String COMMAND_NAME = "network";
  private static final String COMMAND_DESCRIPTION = "Outputs an image to show the reply relationships between the users in the chat.";

  private static final int DEFAULT_NETWORK_TIMERANGE_DAYS = 1;
  private static final int DEFAULT_NETWORK_EDGES_LIMIT = 50;
  private static final int DEFAULT_NETWORK_IMPLICIT_REPLY_RANGE = 10;
  // Please make sure the description in the response text match the colors.
  private static final String EXPLICIT_REPLY_EDGE_COLOR = "red";
  private static final String IMPLICIT_REPLY_EDGE_COLOR = "blue";

  private DatabaseInterface database;
  private GraphVisualizationInterface graphVisualizationUtil;
  private String tempDir;

  public NetworkHandler(MessageAnalysisBot bot, DatabaseInterface database,
      GraphVisualizationInterface graphVisualizationUtil, String tempDir) {
    super(bot);
    this.database = database;
    this.graphVisualizationUtil = graphVisualizationUtil;
    this.tempDir = tempDir;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String getHelpString() {
    return getCommonsCLIHelpString(COMMAND_NAME, COMMAND_DESCRIPTION,
        new Options());
  }

  @Override
  public void handle(long receiverChatId, long dataSourceChatId,
      String[] arguments, Message message, Locale locale) {
    ResourceBundle responseResource = ResourceBundle
        .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
    try {
      // Query database.
      long startEpochSeconds = getTimeSecondsNDaysAgo(
          DEFAULT_NETWORK_TIMERANGE_DAYS);
      long endEpochSeconds = Long.MAX_VALUE;
      database.openConnection();
      List<Map.Entry<Long, Long>> replyList = database
          .getReplyList(dataSourceChatId, startEpochSeconds, endEpochSeconds);
      Map<Long, User> userMap = database.getUsers();
      database.closeConnection();

      // Compute scores.
      Map<ReplyStats, Double> replyScoreMap = new HashMap<>();
      for (int i = 0; i < replyList.size(); i++) {
        long userId = replyList.get(i).getKey();
        long replyToUserId = replyList.get(i).getValue();
        boolean isExplicitReply = replyToUserId != 0;
        if (isExplicitReply) {
          // Only look at the first 3 fields. Keep score as 0.
          ReplyStats key = new ReplyStats(userId, replyToUserId,
              isExplicitReply, 0.0);
          if (!replyScoreMap.containsKey(key)) {
            replyScoreMap.put(key, 1.0);
          } else {
            replyScoreMap.put(key, replyScoreMap.get(key) + 1.0);
          }
        } else {
          int startIndex = Math.max(0,
              i - DEFAULT_NETWORK_IMPLICIT_REPLY_RANGE);
          for (int j = startIndex; j < i; j++) {
            long implicitReplyToUserId = replyList.get(j).getKey();
            // Only look at the first 3 fields. Keep score as 0.
            ReplyStats key = new ReplyStats(userId, implicitReplyToUserId,
                isExplicitReply, 0.0);
            if (implicitReplyToUserId == userId) {
              continue; // Skip if the user implicitly replied to himself.
            }
            if (!replyScoreMap.containsKey(key)) {
              replyScoreMap.put(key,
                  1.0 / DEFAULT_NETWORK_IMPLICIT_REPLY_RANGE);
            } else {
              replyScoreMap.put(key, replyScoreMap.get(key)
                  + 1.0 / DEFAULT_NETWORK_IMPLICIT_REPLY_RANGE);
            }
          }
        }
      }
      if (replyScoreMap.isEmpty()) {
        bot.sendTextMessage(receiverChatId,
            new MessageFormat(responseResource.getString("network.emptyResult"),
                locale).format(
                    new Object[] { DEFAULT_NETWORK_TIMERANGE_DAYS * 24 }));
        return;
      }
      // Sort by score in descending order.
      List<Map.Entry<ReplyStats, Double>> replyScoreList = new ArrayList<>(
          replyScoreMap.entrySet());
      Collections.sort(replyScoreList,
          new Comparator<Map.Entry<ReplyStats, Double>>() {
            public int compare(Map.Entry<ReplyStats, Double> o1,
                Map.Entry<ReplyStats, Double> o2) {
              return o2.getValue().compareTo(o1.getValue());
            }
          });
      if (replyScoreList.size() > DEFAULT_NETWORK_EDGES_LIMIT) {
        replyScoreList = replyScoreList.subList(0, DEFAULT_NETWORK_EDGES_LIMIT);
      }

      // Build graph.
      Graph<User, ReplyStats> graph = new DirectedPseudograph<User, ReplyStats>(
          ReplyStats.class);
      Set<Long> alreadyAddedUserIds = new HashSet<>();
      for (Map.Entry<ReplyStats, Double> entry : replyScoreList) {
        ReplyStats replyStats = entry.getKey();
        long userId = replyStats.getUserId();
        long replyToUserId = replyStats.getReplyToUserId();
        User user = userMap.get(userId);
        User replyToUser = userMap.get(replyToUserId);
        // Note: Bot users are not included in the database query result, so
        // user and replyToUser could be null.
        if (user != null && replyToUser != null) {
          if (!alreadyAddedUserIds.contains(userId)) {
            graph.addVertex(user);
            alreadyAddedUserIds.add(userId);
          }
          if (!alreadyAddedUserIds.contains(replyToUserId)) {
            graph.addVertex(replyToUser);
            alreadyAddedUserIds.add(replyToUserId);
          }
          replyStats.setScore(entry.getValue());
          graph.addEdge(user, replyToUser, replyStats);
        }
      }

      // Generate image.
      File imageFile = File.createTempFile(
          NetworkHandler.class.toString() + "_", ".tmpimg", new File(tempDir));
      graphVisualizationUtil.visualizeReplyRelationship(graph,
          EXPLICIT_REPLY_EDGE_COLOR, IMPLICIT_REPLY_EDGE_COLOR,
          imageFile.getAbsolutePath());

      // Build response.
      bot.sendPhotoMessage(receiverChatId, imageFile.getAbsolutePath(),
          new MessageFormat(responseResource.getString("network.description"),
              locale)
                  .format(new Object[] { DEFAULT_NETWORK_EDGES_LIMIT,
                      DEFAULT_NETWORK_TIMERANGE_DAYS * 24,
                      DEFAULT_NETWORK_IMPLICIT_REPLY_RANGE }));

      imageFile.delete();

    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

  private long getTimeSecondsNDaysAgo(int n) {
    Date now = new Date();
    return (now.getTime() - n * DateUtils.MILLIS_PER_DAY)
        / DateUtils.MILLIS_PER_SECOND;
  }

}
