package com.neoshell.telegram.messageanalysisbot.handler;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.telegram.telegrambots.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class HistoryHandler extends Handler {

  private static final String COMMAND_NAME = "history";
  private static final String COMMAND_DESCRIPTION = "Outputs the history of chat titles and commands.";

  private static final int MAX_NUM_MESSAGES = 100;
  private static final int DEFAULT_NUM_MESSAGES = 10;
  private static final boolean DEFAULT_SHOW_USER = false;
  private static final MessageType DEFAULT_MESSAGE_TYPE = MessageType.CHAT_TITLE;
  @SuppressWarnings("serial")
  private static final Set<MessageType> VALID_TYPES = new HashSet<MessageType>() {
    {
      add(MessageType.CHAT_TITLE);
      add(MessageType.COMMAND);
    }
  };

  private DatabaseInterface database;
  private Options commandOptions;
  private CommandLineParser commandLineParser;
  private ResourceBundle responseResource;

  public HistoryHandler(MessageAnalysisBot bot, DatabaseInterface database) {
    super(bot);
    this.database = database;
    commandOptions = new Options();
    commandOptions.addOption("t", "type", true,
        "The type of messages. Valid args: " + VALID_TYPES.toString());
    commandOptions.addOption("n", "number", true,
        "The number of latest results you want to show. Range:(0,"
            + MAX_NUM_MESSAGES + "].");
    commandOptions.addOption("u", "user", false,
        "Set this option if you want to show the sender of each message.");
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
    responseResource = ResourceBundle.getBundle(RESPONSE_RESOURCE_BUNDLE,
        locale);
    try {
      // Parse command.
      int numMessage = DEFAULT_NUM_MESSAGES;
      MessageType type = DEFAULT_MESSAGE_TYPE;
      boolean showUser = DEFAULT_SHOW_USER;
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        String typeString = commandLine.getOptionValue("t");
        if (typeString != null) {
          type = MessageType.parseFromString(typeString);
          if (!VALID_TYPES.contains(type)) {
            throw new Exception();
          }
        }
        String numCommandsString = commandLine.getOptionValue("n");
        if (numCommandsString != null) {
          numMessage = Integer.parseInt(numCommandsString);
          if (numMessage <= 0 || numMessage > MAX_NUM_MESSAGES) {
            throw new Exception();
          }
        }
        showUser = commandLine.hasOption("u");
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("common.invalidCommand"));
        sendHelpMessage(receiverChatId);
        return;
      }

      // Query database.
      database.openConnection();
      // Get latest commands in ascending order of time.
      List<com.neoshell.telegram.messageanalysisbot.Message> messages = database
          .getMessagesSortedByTime(dataSourceChatId, EnumSet.of(type),
              numMessage, false, true);
      Map<Long, User> userMap = database.getUsers();
      database.closeConnection();

      // Send response.
      if (messages.size() > 0) {
        StringBuilder sendText = new StringBuilder();
        String historyName = getHistoryName(type, locale);
        if (historyName != null) {
          String responseText = historyName + "\n"
              + new MessageFormat(
                  responseResource.getString("common.numLatestMessages"),
                  locale).format(new Object[] { messages.size() })
              + "\n\n";
          sendText.append(responseText);
        }
        for (com.neoshell.telegram.messageanalysisbot.Message m : messages) {
          long userId = m.getUserId();
          String userFullName = userMap.get(userId).getFullName();
          sendText.append(m.getContent());
          if (showUser) {
            sendText.append("\n    ——" + userFullName);
          }
          sendText.append("\n");
        }
        bot.sendTextMessage(receiverChatId, sendText.toString());
      } else {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("common.noMessageWithType")
                + type.toString());
      }
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

  private String getHistoryName(MessageType messageType, Locale locale) {
    switch (messageType) {
    case COMMAND:
      return responseResource.getString("history.title.command");
    case CHAT_TITLE:
      return responseResource.getString("history.title.chatTitle");
    default:
      break;
    }
    return null;
  }

}
