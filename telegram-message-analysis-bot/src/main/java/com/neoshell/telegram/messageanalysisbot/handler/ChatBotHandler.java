package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.CommandUtil;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.chatbot.ChatBotInterface;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

public class ChatBotHandler extends Handler {

  private static final String COMMAND_NAME = "chat";
  private static final String COMMAND_DESCRIPTION = "Chat with the bot.";

  private DatabaseInterface database;
  private ChatBotInterface chatBot;

  public ChatBotHandler(MessageAnalysisBot bot, DatabaseInterface database,
      ChatBotInterface chatBot) {
    super(bot);
    this.database = database;
    this.chatBot = chatBot;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String getHelpString() {
    return getCommonsCLIHelpString(COMMAND_NAME + " [text]",
        COMMAND_DESCRIPTION, new Options());
  }

  @Override
  public void handle(long receiverChatId, long dataSourceChatId,
      String[] arguments, Message message, Locale locale) {
    try {
      // Parse command.
      String text = message.getText().replaceFirst(
          CommandUtil.NON_CLICKABLE_COMMAND_PREFIX + COMMAND_NAME + "\\s*", "");
      if (text.isEmpty()) {
        sendHelpMessage(receiverChatId);
        return;
      }

      String chatBotUserId = "telegram_" + Long.toString(receiverChatId);
      ChatBotReply chatBotReply = chatBot.chat(chatBotUserId, text);
      if (chatBotReply != null) {
        bot.sendTextMessage(receiverChatId, chatBotReply.getText());
      } else { // TODO: handle more response types.
        database.openConnection();
        ResourceBundle responseResource = ResourceBundle
            .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
        database.closeConnection();
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("chat.incompatibleResponse"));
      }
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
