package com.neoshell.telegram.messageanalysisbot.offline;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.ini4j.Wini;

import com.neoshell.telegram.messageanalysisbot.CustomizedLogger;
import com.neoshell.telegram.messageanalysisbot.Message;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;
import com.neoshell.telegram.messageanalysisbot.database.MySQLDatabase;
import com.neoshell.telegram.messageanalysisbot.nlp.NLPInterface;
import com.neoshell.telegram.messageanalysisbot.nlp.NLPUtilClientWrapper;

public class WordCounter {

  private static final boolean COUNT_STOP_WORDS = false;
  private static final String LOG_FILE_NAME_PATTERN = "word_count_%g.log";
  private static Logger logger;

  private DatabaseInterface database;
  private NLPInterface nlpUtil;
  private int numWordPerChatForMonthlyCount;

  public WordCounter(String configFile) throws Exception {
    logger.info("Initializing WordCounter...");
    loadConfig(configFile);
    nlpUtil.initialize();
    database.openConnection();
    logger.info("WordCounter ready to work.");
  }

  public void run() throws Exception {
    List<Long> chatIds = database.getChatIds();
    for (long chatId : chatIds) {
      int maxMonthIndex = database.getMaxWordCountTimeRangeIndex(chatId);
      int currentMonthIndex = getCurrentMonthIndex();
      for (int monthIndex = maxMonthIndex
          + 1; monthIndex < currentMonthIndex; monthIndex++) {
        logger.info("Processing chat:" + chatId + ", year:"
            + (monthIndex / 12 + 1970) + ", month:" + (monthIndex % 12 + 1));
        long startEpochSeconds = convertMonthIndexToEpochSeconds(monthIndex);
        long endEpochSeconds = convertMonthIndexToEpochSeconds(monthIndex + 1);
        List<Message> messages = database.getMessages(chatId,
            Arrays.asList(MessageType.TEXT), startEpochSeconds,
            endEpochSeconds);
        List<String> texts = new ArrayList<>();
        for (Message message : messages) {
          texts.add(message.getContent());
        }
        List<Map.Entry<String, Long>> wordCountList = nlpUtil.countWords(texts,
            COUNT_STOP_WORDS, numWordPerChatForMonthlyCount);
        // Write to database.
        for (Map.Entry<String, Long> wordCount : wordCountList) {
          try {
            database.addOrUpdateWordCount(chatId, monthIndex,
                wordCount.getKey(), wordCount.getValue());
          } catch (SQLException e) {
            logger.severe(ExceptionUtils.getStackTrace(e));
          }
        }
      }
    }
    logger.info("Task finished.");
  }

  public void shutdown() throws Exception {
    database.closeConnection();
    nlpUtil.shutdown();
  }

  private void loadConfig(String configFile) throws Exception {
    Wini config = new Wini(new File(configFile));

    // MySql.
    String databaseUrl = config.get("MySQL", "DatabaseUrl", String.class);
    String databaseUsername = config.get("MySQL", "DatabaseUsername",
        String.class);
    String databasePassword = config.get("MySQL", "DatabasePassword",
        String.class);
    database = new MySQLDatabase(databaseUrl, databaseUsername,
        databasePassword);

    // NLP.
    String nlpUtilServerHost = config.get("NLP", "NLPUtilServerHost",
        String.class);
    int nlpUtilServerPort = config.get("NLP", "NLPUtilServerPort", int.class);
    nlpUtil = new NLPUtilClientWrapper(nlpUtilServerHost, nlpUtilServerPort);
    numWordPerChatForMonthlyCount = config.get("NLP",
        "NumWordPerChatForMonthlyCount", int.class);
  }

  private long convertMonthIndexToEpochSeconds(int monthIndex) {
    int year = monthIndex / 12 + 1970;
    int month = monthIndex % 12; // [0,11]
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(Calendar.MONTH, month);
    calendar.set(Calendar.YEAR, year);
    return calendar.getTimeInMillis() / 1000L;
  }

  private int getCurrentMonthIndex() {
    LocalDate today = LocalDate.now();
    int year = today.getYear();
    int month = today.getMonthValue() - 1; // [0,11]
    return (year - 1970) * 12 + month;
  }

  public static void main(String[] args) {
    try {
      logger = CustomizedLogger.getLogger(MessageAnalysisBot.class,
          LOG_FILE_NAME_PATTERN);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Failed to create logger.");
      return;
    }

    String configFile = args.length > 0 ? args[0] : "config.ini";
    WordCounter wordCounter;
    try {
      wordCounter = new WordCounter(configFile);
    } catch (Exception e) {
      logger.severe(ExceptionUtils.getStackTrace(e));
      return;
    }
    try {
      wordCounter.run();
    } catch (Exception e) {
      logger.severe(ExceptionUtils.getStackTrace(e));
    } finally {
      try {
        wordCounter.shutdown();
      } catch (Exception e1) {
        logger.severe(ExceptionUtils.getStackTrace(e1));
      }
    }
  }

}
