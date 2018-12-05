package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.telegram.telegrambots.meta.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;

public class HelpHandler extends Handler {

  private static final String COMMAND_NAME = "help";

  Map<String, Handler> handlerMap;

  public HelpHandler(MessageAnalysisBot bot, Map<String, Handler> handlerMap) {
    super(bot);
    this.handlerMap = handlerMap;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String getHelpString() {
    StringBuilder description = new StringBuilder(
        "Show help information for all commands.\nCommand list:");
    for (String commandName : handlerMap.keySet()) {
      description.append("\n  " + commandName);
    }
    return getCommonsCLIHelpString(COMMAND_NAME + " [command name]",
        description.toString(), new Options());
  }

  @Override
  public void handle(long receiverChatId, long dataSourceChatId,
      String[] arguments, Message message, Locale locale) {
    try {
      if (arguments.length > 0 && handlerMap.containsKey(arguments[0])) {
        handlerMap.get(arguments[0]).sendHelpMessage(receiverChatId);
      } else {
        sendHelpMessage(receiverChatId);
      }
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
