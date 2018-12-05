package com.neoshell.telegram.messageanalysisbot.handler;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import org.apache.commons.lang.time.DateUtils;

import org.tc33.jheatchart.HeatChart;

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class TimeStatsHandler extends Handler {

  private static final int DEFAULT_TIME_RANGE_DAYS = 7;
  private static final int MAX_TIME_RANGE_DAYS = 90;
  private static final boolean DEFAULT_NORMALIZATION_OPTION = false;
  private static final int MAX_NUM_USERS = 50;

  private static final String COMMAND_NAME = "timestats";
  private static final String COMMAND_DESCRIPTION = String.format(
      "Outputs an image to show the time distribution of chat messages of the most active users (at most %d).",
      MAX_NUM_USERS);

  private DatabaseInterface database;
  private String tempDir;

  private Options commandOptions;
  private CommandLineParser commandLineParser;

  public TimeStatsHandler(MessageAnalysisBot bot, DatabaseInterface database,
      String tempDir) {
    super(bot);
    this.database = database;
    this.tempDir = tempDir;
    commandOptions = new Options();
    commandOptions.addOption("d", "day", true,
        String.format(
            "The chart will be generated based on the messages in last n days. "
                + "Range: (0, %d]. Default value: %d",
            MAX_TIME_RANGE_DAYS, DEFAULT_TIME_RANGE_DAYS));
    commandOptions.addOption("n", "normalized", false,
        "Set this option if you want to get normalized result.");
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
      ResourceBundle rb = ResourceBundle.getBundle(RESPONSE_RESOURCE_BUNDLE,
          locale);
      int timeRangeDays = DEFAULT_TIME_RANGE_DAYS;
      boolean isNormalized = DEFAULT_NORMALIZATION_OPTION;
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        String dayString = commandLine.getOptionValue("d");
        if (dayString != null) {
          timeRangeDays = Integer.parseInt(dayString);
          if (timeRangeDays <= 0 || timeRangeDays > MAX_TIME_RANGE_DAYS) {
            throw new Exception();
          }
        }
        if (commandLine.hasOption("n")) {
          isNormalized = true;
        }
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId, "Invalid command.");
        sendHelpMessage(receiverChatId);
        return;
      }

      // Query database.
      long startEpochSeconds = getTimeSecondsNDaysAgo(timeRangeDays);
      long endEpochSeconds = Long.MAX_VALUE;
      database.openConnection();
      Map<User, int[]> userTimeDistributionMap = database
          .getTimeDistributionInHour(dataSourceChatId, startEpochSeconds,
              endEpochSeconds);
      if (userTimeDistributionMap.isEmpty()) {
        bot.sendTextMessage(receiverChatId,
            new MessageFormat(rb.getString("timestats.emptyResult"))
                .format(new Object[] { timeRangeDays }));
        database.closeConnection();
        return;
      }
      TimeZone timeZone = getTimeZone(dataSourceChatId, database);
      int hourOffset = (int) (timeZone.getOffset(System.currentTimeMillis())
          / DateUtils.MILLIS_PER_HOUR);
      database.closeConnection();

      // Make heatmap data.
      List<TimeDistributionInfo> timeDistributionInfoList = new ArrayList<>();
      for (Map.Entry<User, int[]> entry : userTimeDistributionMap.entrySet()) {
        User user = entry.getKey();
        int[] timeDistribution = entry.getValue();
        int maxMessageCount = 0;
        int totalMessageCount = 0;
        for (int count : timeDistribution) {
          maxMessageCount = Math.max(maxMessageCount, count);
          totalMessageCount += count;
        }
        timeDistributionInfoList.add(new TimeDistributionInfo(user,
            maxMessageCount, totalMessageCount, timeDistribution));
      }
      // Sort by total message count in descending order.
      Collections.sort(timeDistributionInfoList,
          new Comparator<TimeDistributionInfo>() {
            public int compare(TimeDistributionInfo o1,
                TimeDistributionInfo o2) {
              return Integer.compare(o2.totalMessageCount,
                  o1.totalMessageCount);
            }
          });
      if (timeDistributionInfoList.size() > MAX_NUM_USERS) {
        timeDistributionInfoList = timeDistributionInfoList.subList(0,
            MAX_NUM_USERS);
      }
      double[][] heatmapData = new double[timeDistributionInfoList.size()][24];
      String[] yValues = new String[timeDistributionInfoList.size()];
      for (int y = 0; y < timeDistributionInfoList.size(); y++) {
        TimeDistributionInfo timeDistributionInfo = timeDistributionInfoList
            .get(y);
        yValues[y] = String.format("%s: %d",
            timeDistributionInfo.user.getFullName(),
            timeDistributionInfo.totalMessageCount);
        // Shift to local time zone
        for (int h = 0; h < heatmapData[y].length; h++) {
          int x = (h + 24 + hourOffset) % 24;
          if (isNormalized) {
            heatmapData[y][x] = (double) timeDistributionInfo.timeDistribution[h]
                / timeDistributionInfo.maxMessageCount;
          } else {
            heatmapData[y][x] = timeDistributionInfo.timeDistribution[h];
          }
        }
      }

      // Generate image.
      HeatChart heatmap = new HeatChart(heatmapData);
      heatmap.setTitle(new MessageFormat(rb.getString("timestats.image.title"))
          .format(new Object[] { timeRangeDays }));
      heatmap.setXAxisLabel(
          new MessageFormat(rb.getString("timestats.image.xAxis"))
              .format(new Object[] { timeZone.getDisplayName(locale) }));
      heatmap.setYAxisLabel(rb.getString("timestats.image.yAxis"));
      heatmap.setYValues(yValues);
      File imageFile = File.createTempFile(
          TimeStatsHandler.class.toString() + "_", ".png", new File(tempDir));
      heatmap.saveToFile(imageFile);

      // Build response.
      bot.sendPhotoMessage(receiverChatId, imageFile.getAbsolutePath(),
          /* caption= */null);

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

  private class TimeDistributionInfo {
    public User user;
    public int maxMessageCount;
    public int totalMessageCount;
    public int[] timeDistribution; // UTC hour

    public TimeDistributionInfo(User user, int maxMessageCount,
        int totalMessageCount, int[] timeDistribution) {
      super();
      this.user = user;
      this.maxMessageCount = maxMessageCount;
      this.totalMessageCount = totalMessageCount;
      this.timeDistribution = timeDistribution;
    }

  }

}
