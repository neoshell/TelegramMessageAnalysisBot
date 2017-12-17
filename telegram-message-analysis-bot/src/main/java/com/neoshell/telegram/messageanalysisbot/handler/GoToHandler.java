package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.telegram.telegrambots.api.objects.Message;

import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.ParseMode;

public class GoToHandler extends Handler {

  private static final String COMMAND_NAME = "goto";
  private static final String COMMAND_DESCRIPTION = "Outputs a message which replies to the message you want to go to. By clicking the replied message, you can jump to it.";

  private Options commandOptions;
  private CommandLineParser commandLineParser;

  public GoToHandler(MessageAnalysisBot bot) {
    super(bot);
    commandOptions = new Options();
    OptionGroup optionGroup = new OptionGroup();
    optionGroup.setRequired(true);
    optionGroup.addOption(Option.builder("m").longOpt("message").hasArg()
        .desc("The id of the message you want to go to.").build());
    commandOptions.addOptionGroup(optionGroup);
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
    int replyToMessageId = 0;
    ResourceBundle responseResource = ResourceBundle
        .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
    try {
      // Parse command.
      CommandLine commandLine = null;
      try {
        commandLine = commandLineParser.parse(commandOptions, arguments);
        replyToMessageId = Integer.parseInt(commandLine.getOptionValue("m"));
      } catch (Exception e) {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("common.invalidCommand"));
        sendHelpMessage(receiverChatId);
        return;
      }

      // Send response.
      try {
        bot.sendTextMessage(receiverChatId,
            responseResource.getString("goto.response") + " ID="
                + replyToMessageId,
            ParseMode.NULL, replyToMessageId);
      } catch (Exception e) {
        if (e.toString().contains("reply message not found")) {
          bot.sendTextMessage(receiverChatId,
              responseResource.getString("common.noMessageWithId")
                  + replyToMessageId);
        } else {
          throw e;
        }
      }
    } catch (Exception e) {
      MessageAnalysisBot.getLogger().severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
