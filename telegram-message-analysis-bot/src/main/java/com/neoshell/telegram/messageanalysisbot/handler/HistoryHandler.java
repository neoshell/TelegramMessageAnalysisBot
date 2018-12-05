package com.neoshell.telegram.messageanalysisbot.handler;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
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
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class HistoryHandler extends Handler {

  private static final String COMMAND_NAME = "history";
  private static final String COMMAND_DESCRIPTION = "Outputs the history of chat titles and commands.";

  private static final int MAX_NUM_MESSAGES = 100;
  private static final int DEFAULT_NUM_MESSAGES = 10;
  private static final int MAX_SNIPPET_LENGTH = 80;
  private static final boolean DEFAULT_SHOW_USER = false;
  private static final MessageType DEFAULT_MESSAGE_TYPE = MessageType.CHAT_TITLE;
  @SuppressWarnings("serial")
  private static final Set<MessageType> VALID_TYPES = new HashSet<MessageType>() {
    {
      add(MessageType.CHAT_TITLE);
      add(MessageType.COMMAND);
      add(MessageType.PINNED_MESSAGE);
    }
  };
  private static final String PIN_COMMAND = "/pin";

  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
      "yyyy/MM/dd HH:mm z");

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
              /* contentLike= */null, numMessage, /* isOldest= */false,
              /* isAscending= */true);
      if (type == MessageType.PINNED_MESSAGE) {
        messages = mergeManuallyPinnedMessages(dataSourceChatId, numMessage,
            messages);
      }
      Map<Long, User> userMap = database.getUsers();
      TimeZone timeZone = getTimeZone(dataSourceChatId, database);
      database.closeConnection();

      // Send response.
      if (messages.size() > 0) {
        FORMATTER.setTimeZone(timeZone);
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
          sendText.append(getItemContent(m, type, showUser, userMap));
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
    case PINNED_MESSAGE:
      return responseResource.getString("history.title.pinnedMessage");
    default:
      return null;
    }
  }

  private String getItemContent(
      com.neoshell.telegram.messageanalysisbot.Message message,
      MessageType messageType, boolean showUser, Map<Long, User> userMap) {
    long userId = message.getUserId();
    StringBuilder sb = new StringBuilder();
    switch (messageType) {
    case COMMAND:
    case CHAT_TITLE:
      sb.append(message.getContent());
      break;
    case PINNED_MESSAGE:
      userId = message.getReplyToUserId();
      if (userId == 0) {
        return "";
      }
      sb.append("------\n");
      String timeStr = FORMATTER
          .format(new Date(message.getEpochSeconds() * 1000));
      sb.append(timeStr);
      // Show a goto command if possible.
      long messageId = message.getReplyToMessageId();
      if (messageId > 0) {
        sb.append("    " + CommandUtil.getClickableGotoCommand(messageId));
      }
      sb.append("\n");
      String content = message.getContent();
      if (content == null || content.isEmpty()) {
        sb.append(responseResource.getString("history.text.nonTextMessage"));
      } else {
        sb.append(getSnippet(message.getContent()));
      }
      break;
    default:
      return "";
    }
    if (showUser) {
      User user = userMap.get(userId);
      // user can be null if userId is got from replyToUserId. Usually it means
      // the user is a bot.
      String userFullName = user == null ? "bot" : user.getFullName();
      sb.append("\n    ——" + userFullName);
    }
    return sb.append("\n").toString();
  }

  private String getSnippet(String str) {
    if (str.length() <= MAX_SNIPPET_LENGTH) {
      return str;
    }
    StringBuilder sb = new StringBuilder(
        str.substring(0, MAX_SNIPPET_LENGTH - 3));
    return sb.append("...").toString();
  }

  // Some messages might be pinned by bots using command like "/pin". Load these
  // messages as well.
  private List<com.neoshell.telegram.messageanalysisbot.Message> mergeManuallyPinnedMessages(
      long dataSourceChatId, int numMessage,
      List<com.neoshell.telegram.messageanalysisbot.Message> manuallyPinnedMessages)
      throws SQLException {
    List<com.neoshell.telegram.messageanalysisbot.Message> mergedMessages = new ArrayList<>(
        manuallyPinnedMessages);
    List<com.neoshell.telegram.messageanalysisbot.Message> pinCommandMessages = database
        .getMessagesSortedByTime(dataSourceChatId,
            EnumSet.of(MessageType.COMMAND), /* contentLike= */PIN_COMMAND,
            numMessage, /* isOldest= */false, /* isAscending= */true);
    List<Long> pinnedMessageIds = new ArrayList<>();
    for (com.neoshell.telegram.messageanalysisbot.Message m : pinCommandMessages) {
      pinnedMessageIds.add(m.getReplyToMessageId());
    }
    Map<Long, com.neoshell.telegram.messageanalysisbot.Message> pinnedMessages = database
        .getMessagesById(pinnedMessageIds);
    for (com.neoshell.telegram.messageanalysisbot.Message m : pinCommandMessages) {
      com.neoshell.telegram.messageanalysisbot.Message pinnedMessage = pinnedMessages
          .get(m.getReplyToMessageId());
      if (pinnedMessage != null) {
        // Replace the content so that it can be handled as same as manually
        // pinned messages.
        m.setContent(pinnedMessage.getContent());
      } else {
        m.setContent(
            responseResource.getString("history.text.unrecordedMessage"));
      }
    }
    mergedMessages.addAll(pinCommandMessages);
    // Sort by epoch seconds in ascending order.
    Collections.sort(mergedMessages,
        new Comparator<com.neoshell.telegram.messageanalysisbot.Message>() {
          public int compare(
              com.neoshell.telegram.messageanalysisbot.Message o1,
              com.neoshell.telegram.messageanalysisbot.Message o2) {
            return Long.compare(o1.getEpochSeconds(), o2.getEpochSeconds());
          }
        });
    int size = mergedMessages.size();
    if (size > numMessage) {
      return mergedMessages.subList(size - numMessage, size);
    }
    return mergedMessages;
  }

}
