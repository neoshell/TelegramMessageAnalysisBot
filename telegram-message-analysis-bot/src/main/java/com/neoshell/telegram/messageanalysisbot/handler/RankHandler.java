package com.neoshell.telegram.messageanalysisbot.handler;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.telegram.telegrambots.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class RankHandler extends Handler {

  private static final String COMMAND_NAME = "rank";
  private static final String COMMAND_DESCRIPTION = "Computes a rank based on the number of messages of the given type. By default it outputs a daily rank for text messages.";

  private static final int DEFAULT_REFRESH_HOUR_OF_DAY = 0;
  private static final String[] MEDAL_EMOJI = { "🥇", "🥈", "🥉" };
  private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm z";

  private DatabaseInterface database;

  private Options commandOptions;
  private CommandLineParser commandLineParser;

  public RankHandler(MessageAnalysisBot bot, DatabaseInterface database) {
    super(bot);
    this.database = database;
    commandOptions = new Options();
    commandOptions.addOption("a", "all", false,
        "Set this option if you want to get rank based on all past messages.");
    commandOptions.addOption("t", "type", true,
        "The type of messages. Valid args: " + Arrays.toString(ArrayUtils
            .removeElement(MessageType.values(), MessageType.UNKNOWN)));
    commandLineParser = new DefaultParser();
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String getHelpString() {
    return getCommonsCLIHelpString(COMMAND_NAME, COMMAND_DESCRIPTION,
        commandOptions);
  }

  @Override
  public void handle(long receiverChatId, long dataSourceChatId,
      String[] arguments, Message message, Locale locale) {
    try {
      // Parse arguments.
      boolean isTimeRangeDaily = true;
      MessageType type = MessageType.TEXT;
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        if (commandLine.hasOption("a")) {
          isTimeRangeDaily = false;
        }
        String typeString = commandLine.getOptionValue("t");
        if (typeString != null) {
          type = MessageType.parseFromString(typeString);
          if (type == MessageType.UNKNOWN) {
            throw new Exception();
          }
        }
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId, "Invalid command.");
        sendHelpMessage(receiverChatId);
        return;
      }

      // Query.
      database.openConnection();
      TimeZone timeZone = getTimeZone(dataSourceChatId, database);
      int refreshHour = getRefreshHour(dataSourceChatId);
      long startEpochSeconds = 0L;
      long endEpochSeconds = Long.MAX_VALUE;
      if (isTimeRangeDaily) {
        startEpochSeconds = computeStartTimeSeconds(refreshHour, 0, timeZone);
        endEpochSeconds = startEpochSeconds + 24 * 3600; // 24h
      }
      List<Map.Entry<User, Integer>> rank = database.getRank(dataSourceChatId,
          startEpochSeconds, endEpochSeconds, type.toString());
      if (!isTimeRangeDaily && !rank.isEmpty()) {
        List<com.neoshell.telegram.messageanalysisbot.Message> oldestMessages = database
            .getMessagesSortedByTime(dataSourceChatId, EnumSet.of(type), 1,
                true, true);
        startEpochSeconds = oldestMessages.get(0).getEpochSeconds();
      }
      database.closeConnection();

      // Build response.
      ResourceBundle rb = ResourceBundle.getBundle(RESPONSE_RESOURCE_BUNDLE,
          locale);
      SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
      formatter.setTimeZone(timeZone);
      StringBuilder sendText = new StringBuilder();
      String rankName = "🏆 " + getRankTitle(type, locale);
      if (rankName != null) {
        sendText.append(rankName + "\n\n");
      }
      String startTimeStr = formatter
          .format(new Date(startEpochSeconds * 1000));
      if (isTimeRangeDaily) {
        String endTimeStr = formatter.format(new Date(endEpochSeconds * 1000));
        sendText.append(rb.getString("rank.text.daily") + "\n");
        sendText.append(
            rb.getString("rank.text.from") + ": " + startTimeStr + "\n");
        sendText
            .append(rb.getString("rank.text.to") + ": " + endTimeStr + "\n\n");
      } else {
        sendText.append(rb.getString("rank.text.total") + "\n");
        sendText.append(
            rb.getString("rank.text.from") + ": " + startTimeStr + "\n\n");
      }
      for (int i = 0; i < rank.size(); i++) {
        Map.Entry<User, Integer> item = rank.get(i);
        if (i < 3) { // Use medal-shaped emoji for the top 3 users.
          sendText.append(MEDAL_EMOJI[i] + "  ");
        } else {
          sendText.append(" " + (i + 1) + ".  ");
        }
        sendText.append(
            item.getKey().getFullName() + ": " + item.getValue() + "\n");
      }
      if (rank.isEmpty()) {
        sendText.append(
            rb.getString("rank.text.emptyResult") + ": " + type.toString());
      }
      bot.sendTextMessage(receiverChatId, sendText.toString());
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

  private int getRefreshHour(long chatId) throws SQLException {
    String refreshHourString = database.getOption(chatId, "rank_refresh_hour");
    return refreshHourString != null ? Integer.parseInt(refreshHourString)
        : DEFAULT_REFRESH_HOUR_OF_DAY;
  }

  private long computeStartTimeSeconds(int refreshHourOfDay, int minusDays,
      TimeZone timeZone) {
    LocalDateTime localDateTime = LocalDateTime.now(timeZone.toZoneId());
    int currentHourOfDay = localDateTime.getHour();
    localDateTime = LocalDateTime.of(localDateTime.getYear(),
        localDateTime.getMonth(), localDateTime.getDayOfMonth(),
        refreshHourOfDay, 0);
    if (currentHourOfDay < refreshHourOfDay) {
      localDateTime = localDateTime.minusDays(1);
    }
    localDateTime = localDateTime.minusDays(minusDays);
    return localDateTime
        .toEpochSecond(timeZone.toZoneId().getRules().getOffset(Instant.now()));
  }

  private String getRankTitle(MessageType messageType, Locale locale) {
    ResourceBundle responseResource = ResourceBundle
        .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
    switch (messageType) {
    case TEXT:
      return responseResource.getString("rank.title.text");
    case STICKER:
      return responseResource.getString("rank.title.sticker");
    case GIF:
      return responseResource.getString("rank.title.gif");
    case IMAGE:
      return responseResource.getString("rank.title.image");
    case VIDEO:
      return responseResource.getString("rank.title.video");
    case AUDIO:
      return responseResource.getString("rank.title.audio");
    case VOICE:
      return responseResource.getString("rank.title.voice");
    case COMMAND:
      return responseResource.getString("rank.title.command");
    case CHAT_TITLE:
      return responseResource.getString("rank.title.chatTitle");
    case CHAT_PHOTO:
      return responseResource.getString("rank.title.chatPhoto");
    default:
      break;
    }
    return null;
  }

}
