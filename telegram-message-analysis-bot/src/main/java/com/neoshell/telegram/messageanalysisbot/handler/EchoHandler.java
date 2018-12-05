package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.Locale;

import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.CommandUtil;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;

public class EchoHandler extends Handler {

  private static final String COMMAND_NAME = "echo";
  private static final String COMMAND_DESCRIPTION = "Outputs the strings being passed as arguments.";

  public EchoHandler(MessageAnalysisBot bot) {
    super(bot);
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
      String text = message.getText().replaceFirst(
          CommandUtil.NON_CLICKABLE_COMMAND_PREFIX + COMMAND_NAME + "\\s*", "");
      if (!text.isEmpty()) {
        bot.sendTextMessage(receiverChatId, text);
      } else {
        sendHelpMessage(receiverChatId);
      }
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
