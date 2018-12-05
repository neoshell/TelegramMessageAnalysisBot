package com.neoshell.telegram.messageanalysisbot.handler;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.CommandUtil;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;
import com.neoshell.telegram.messageanalysisbot.nlp.NLPInterface;

public class KeywordHandler extends Handler {

  private static final String COMMAND_NAME = "keyword";
  private static final String COMMAND_DESCRIPTION = "Computes the keywords of messages in the given time range.";

  private static final int DEFAULT_KEYWORDS_MESSAGE_RANGE = 500;
  private static final int MAX_KEYWORDS_MESSAGE_RANGE = 3000;
  private static final int WORD_COUNT_MAP_LIMIT = 500;

  private DatabaseInterface database;
  private NLPInterface nlpUtil;

  private Options commandOptions;
  private CommandLineParser commandLineParser;

  public KeywordHandler(MessageAnalysisBot bot, DatabaseInterface database,
      NLPInterface nlpUtil) {
    super(bot);
    this.database = database;
    this.nlpUtil = nlpUtil;
    commandOptions = new Options();
    commandOptions.addOption("n", "number", true,
        "The number of messages you want to compute keywords for. "
            + "Range: (0," + MAX_KEYWORDS_MESSAGE_RANGE + "].");
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
    ResourceBundle responseResource = ResourceBundle
        .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
    try {
      // Parse argument.
      int messageRange = DEFAULT_KEYWORDS_MESSAGE_RANGE;
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        String messageRangeString = commandLine.getOptionValue("n");
        if (messageRangeString != null) {
          messageRange = Integer.parseInt(messageRangeString);
          if (messageRange <= 0 || messageRange > MAX_KEYWORDS_MESSAGE_RANGE) {
            throw new Exception();
          }
        }
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("common.invalidCommand"));
        sendHelpMessage(receiverChatId);
        return;
      }

      // Query.
      database.openConnection();
      Map<String, Long> wordCountMap = null;
      try {
        int startMonthIndex = 0;
        int endMonthIndex = Integer.MAX_VALUE;
        wordCountMap = database.getWordCount(dataSourceChatId, startMonthIndex,
            endMonthIndex, WORD_COUNT_MAP_LIMIT);
      } catch (Exception e) {
        wordCountMap = new HashMap<>();
      }
      // Get latest texts in ascending order of time.
      List<com.neoshell.telegram.messageanalysisbot.Message> latestMessages = database
          .getMessagesSortedByTime(dataSourceChatId,
              Arrays.asList(MessageType.values()), /* contentLike= */null,
              messageRange, /* isOldest= */false, /* isAscending= */true);
      TimeZone timeZone = getTimeZone(dataSourceChatId, database);
      database.closeConnection();

      // Compute keywords.
      nlpUtil.initialize();
      List<KeywordInfo> keywordInfoList = nlpUtil
          .computeKeywords(latestMessages, wordCountMap);
      nlpUtil.shutdown();

      // Send response.
      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm z");
      formatter.setTimeZone(timeZone);
      StringBuilder sendTextBuilder = new StringBuilder();
      for (KeywordInfo keywordInfo : keywordInfoList) {
        sendTextBuilder.append("------\n");
        String startTimeStr = formatter
            .format(new Date(keywordInfo.getStartTimeSeconds() * 1000));
        sendTextBuilder.append(startTimeStr);
        // Show a goto command if possible.
        long messageId = keywordInfo.getFirstMessageId();
        if (messageId > 0) {
          sendTextBuilder
              .append("    " + CommandUtil.getClickableGotoCommand(messageId));
        }
        sendTextBuilder.append("\n");
        for (String keyword : keywordInfo.getKeywords()) {
          if (keyword.startsWith("//")) { // Remove url.
            continue;
          }
          sendTextBuilder.append(keyword + ", ");
        }
        sendTextBuilder.append("\n");
      }
      String sendText = sendTextBuilder.length() > 0
          ? new MessageFormat(responseResource.getString("keyword.title"),
              locale).format(new Object[] { messageRange }) + "\n"
              + sendTextBuilder.toString()
          : responseResource.getString("keyword.emptyResult");
      bot.sendTextMessage(receiverChatId, sendText);
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
