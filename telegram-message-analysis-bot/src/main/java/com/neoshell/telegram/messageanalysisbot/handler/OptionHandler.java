package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.Language;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class OptionHandler extends Handler {

  private static final String COMMAND_NAME = "option";
  private static final String COMMAND_DESCRIPTION = "Sets bot options.";

  private DatabaseInterface database;
  private Options commandOptions;
  private CommandLineParser commandLineParser;

  public OptionHandler(MessageAnalysisBot bot, DatabaseInterface database) {
    super(bot);
    this.database = database;
    commandOptions = new Options();
    commandOptions.addOption("L", "language", true,
        "Language. Valid args: " + Arrays.toString(
            ArrayUtils.removeElement(Language.values(), Language.UNKNOWN)));
    commandOptions.addOption("T", "timezone", true, "Timezone ID. e.g. EST.");
    commandOptions.addOption("R", "rank_refresh", true,
        "The hour of day when ranks get refreshed. Expect an integer from 0 to 23.");
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
      // Parse command.
      List<Map.Entry<String, String>> optionsToUpdate = new ArrayList<>();
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        if (commandLine.hasOption("L")) {
          String optionValue = commandLine.getOptionValue("L");
          Language language = Language.parseFromString(optionValue);
          if (language == Language.UNKNOWN) {
            throw new Exception();
          }
          optionsToUpdate.add(new AbstractMap.SimpleEntry<String, String>(
              "language", optionValue));
        }
        if (commandLine.hasOption("T")) {
          String optionValue = commandLine.getOptionValue("T");
          if (!isValidTimezoneId(optionValue)) {
            throw new Exception();
          }
          optionsToUpdate.add(new AbstractMap.SimpleEntry<String, String>(
              "timezone", optionValue));
        }
        if (commandLine.hasOption("R")) {
          String optionValue = commandLine.getOptionValue("R");
          int rankRefreshHour = Integer.parseInt(optionValue);
          if (rankRefreshHour < 0 || rankRefreshHour > 23) {
            throw new Exception();
          }
          optionsToUpdate.add(new AbstractMap.SimpleEntry<String, String>(
              "rank_refresh_hour", optionValue));
        }
        if (optionsToUpdate.isEmpty()) {
          throw new Exception();
        }
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("common.invalidCommand"));
        sendHelpMessage(receiverChatId);
        return;
      }

      // Write to database.
      database.openConnection();
      for (Map.Entry<String, String> option : optionsToUpdate) {
        database.addOrUpdateOptions(dataSourceChatId, option.getKey(),
            option.getValue());
      }
      database.closeConnection();

      // Send response.
      bot.sendTextMessage(receiverChatId,
          responseResource.getString("option.success") + ": "
              + optionsToUpdate.toString());

    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

  private boolean isValidTimezoneId(String id) {
    String[] validTimezoneIds = TimeZone.getAvailableIDs();
    for (int i = 0; i < validTimezoneIds.length; i++) {
      if (id.equals(validTimezoneIds[i])) {
        return true;
      }
    }
    return false;
  }

}
