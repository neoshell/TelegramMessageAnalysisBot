package com.neoshell.telegram.messageanalysisbot.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.neoshell.telegram.messageanalysisbot.MarkdownFormatter;
import com.neoshell.telegram.messageanalysisbot.MessageAnalysisBot;
import com.neoshell.telegram.messageanalysisbot.ParseMode;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;

/**
 * Steps of adding a new handler:
 * 1. Create a new class which extends Handler class. Implement all abstract
 *    methods.
 * 2. Register the new handler in the method registerHandlers() of
 *    MessageAnalysisBot class.
 */
public abstract class Handler {

  protected static final String RESPONSE_RESOURCE_BUNDLE = MessageAnalysisBot.RESPONSE_RESOURCE_BUNDLE;
  protected static final int HELP_LINE_WIDTH = MessageAnalysisBot.NUM_FIEXED_WIDTH_CHAR_PER_LINE;
  // Amount of padding to the left of each line.
  protected static final int HELP_LEFT_PADDING = 2;
  // Amount of padding between each option name and the description.
  protected static final int HELP_DESC_PADDING = 2;

  protected MessageAnalysisBot bot;

  public Handler(MessageAnalysisBot bot) {
    this.bot = bot;
  }

  /**
   * @return The command name.
   */
  public abstract String getCommandName();

  /**
   * @return The help information.
   */
  public abstract String getHelpString();

  /**
   * Processes the input message and sends response.
   * 
   * @param receiverChatId
   *          The chat id to receive the response.
   * @param dataSourceChatId
   *          The chat id for getting data sources. It could be different from
   *          receiverChatId in debug mode.
   * @param arguments
   * @param message
   *          The input message.
   */
  public abstract void handle(long receiverChatId, long dataSourceChatId,
      String[] arguments, Message message, Locale locale);

  /**
   * Sends a pre-formatted fixed-width help message.
   * 
   * @param chatId
   * @throws TelegramApiException
   */
  protected void sendHelpMessage(long chatId) throws TelegramApiException {
    String formattedString = MarkdownFormatter.codeBlock(getHelpString());
    bot.sendTextMessage(chatId, formattedString, ParseMode.MARKDOWN);
  }

  protected String getCommonsCLIHelpString(String commandSyntax,
      String description, Options options) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(pw, HELP_LINE_WIDTH, commandSyntax, description,
        options, HELP_LEFT_PADDING, HELP_DESC_PADDING, null);
    return sw.toString();
  }

  /**
   * Gets time zone option of the chat. Please make sure database connection is
   * open before calling this method.
   * 
   * @param chatId
   * @param database
   * @return
   * @throws SQLException
   */
  protected TimeZone getTimeZone(long chatId, DatabaseInterface database)
      throws SQLException {
    String timezoneString = database.getOption(chatId, "timezone");
    return timezoneString != null ? TimeZone.getTimeZone(timezoneString)
        : bot.getDefaultTimeZone();
  }

}
