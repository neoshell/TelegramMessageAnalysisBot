package com.neoshell.telegram.messageanalysisbot;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.ini4j.Wini;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.neoshell.telegram.messageanalysisbot.database.MySQLDatabase;
import com.neoshell.telegram.messageanalysisbot.chatbot.ChatBotInterface;
import com.neoshell.telegram.messageanalysisbot.chatbot.TuringRobot;
import com.neoshell.telegram.messageanalysisbot.database.DatabaseInterface;
import com.neoshell.telegram.messageanalysisbot.handler.EchoHandler;
import com.neoshell.telegram.messageanalysisbot.handler.GoToHandler;
import com.neoshell.telegram.messageanalysisbot.handler.Handler;
import com.neoshell.telegram.messageanalysisbot.handler.HelpHandler;
import com.neoshell.telegram.messageanalysisbot.handler.HistoryHandler;
import com.neoshell.telegram.messageanalysisbot.handler.KeywordHandler;
import com.neoshell.telegram.messageanalysisbot.handler.NetworkHandler;
import com.neoshell.telegram.messageanalysisbot.handler.OptionHandler;
import com.neoshell.telegram.messageanalysisbot.handler.RankHandler;
import com.neoshell.telegram.messageanalysisbot.handler.TimeStatsHandler;
import com.neoshell.telegram.messageanalysisbot.handler.ChatBotHandler;
import com.neoshell.telegram.messageanalysisbot.nlp.NLPInterface;
import com.neoshell.telegram.messageanalysisbot.nlp.NLPUtilClientWrapper;
import com.neoshell.telegram.messageanalysisbot.visualization.GraphVisualizationInterface;
import com.neoshell.telegram.messageanalysisbot.visualization.Graphviz;

public class MessageAnalysisBot extends TelegramLongPollingBot {

  public static final int MAX_MESSAGE_LENGTH = 4096;
  public static final int NUM_FIEXED_WIDTH_CHAR_PER_LINE = 50;
  public static final String RESPONSE_RESOURCE_BUNDLE = "response";

  private static final String LOG_FILE_NAME_PATTERN = "bot_%g.log";
  private static Logger logger;
  private static final String DEFAULT_CONFIG_PATH = "config.ini";
  private static final String LANGUAGE_OPTION_NAME = "language";

  private Map<String, Handler> handlerMap;

  private String botUsername;
  private String botToken;
  private Set<Long> chatWhiteList;
  private Set<Long> debugUsers; // Users who can use debug mode.
  private Locale defaultLocale;
  private TimeZone defaultTimeZone;
  private String tempDir;

  private DatabaseInterface database;
  private NLPInterface nlpUtil;
  private GraphVisualizationInterface graphVisualizationUtil;
  private ChatBotInterface chatBot;

  public static Logger getLogger() {
    return logger;
  }

  public MessageAnalysisBot(String configFile) {
    logger.info("Initializing MessageAnalysisBot...");
    try {
      handlerMap = new HashMap<>();
      loadConfig(configFile);
      registerHandlers();
    } catch (Exception e) {
      logger.severe(ExceptionUtils.getStackTrace(e));
      System.exit(0);
    }
    logger.info("MessageAnalysisBot ready to work.");
  }

  @Override
  public String getBotUsername() {
    return botUsername;
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      Message message = update.getMessage();
      long chatId = message.getChatId();
      if (!isChatInWhiteList(chatId)) {
        logger.info("Update from unauthorized chat: " + chatId);
        try {
          sendTextMessage(chatId,
              "To use this bot, please contact the bot owner and whitelist the group id: "
                  + chatId);
        } catch (Exception e) {
          logger.severe(ExceptionUtils.getStackTrace(e));
        }
        return;
      }

      try {
        MessageType messageType = handleMessage(message);
        saveMessage(message, messageType);
      } catch (Exception e) {
        logger.severe(ExceptionUtils.getStackTrace(e));
      }
    }
  }

  // If the text is longer than MAX_MESSAGE_LENGTH, it will split the text and
  // send through multiple messages.
  public void sendTextMessage(long chatId, String text, ParseMode parseMode,
      int replyToMessageId) throws TelegramApiException {
    int num = text.length() / MAX_MESSAGE_LENGTH + 1;
    for (int i = 0; i < num; i++) {
      SendMessage sendMessage = new SendMessage();
      sendMessage.setChatId(Long.valueOf(chatId).toString());
      sendMessage.setParseMode(parseMode.toString());
      sendMessage.setText(text.substring(MAX_MESSAGE_LENGTH * i,
          Math.min(text.length(), MAX_MESSAGE_LENGTH * (i + 1))));
      if (replyToMessageId > 0) {
        sendMessage.setReplyToMessageId(replyToMessageId);
      }
      execute(sendMessage);
    }
  }

  public void sendTextMessage(long chatId, String text, ParseMode parseMode)
      throws TelegramApiException {
    sendTextMessage(chatId, text, parseMode, 0);
  }

  public void sendTextMessage(long chatId, String text)
      throws TelegramApiException {
    sendTextMessage(chatId, text, ParseMode.NULL, 0);
  }

  public void sendPhotoMessage(long chatId, String photoPath, String caption)
      throws TelegramApiException {
    SendPhoto sendPhoto = new SendPhoto();
    sendPhoto.setChatId(Long.valueOf(chatId).toString());
    sendPhoto.setPhoto(new InputFile(new File(photoPath)));
    sendPhoto.setCaption(caption);
    execute(sendPhoto);
  }

  public String getFileUrl(String fileId) throws TelegramApiException {
    GetFile getFile = new GetFile();
    getFile.setFileId(fileId);
    return execute(getFile).getFileUrl(botToken);
  }

  public TimeZone getDefaultTimeZone() {
    return defaultTimeZone;
  }

  private void loadConfig(String configFile) throws Exception {
    Wini config = new Wini(new File(configFile));

    botUsername = config.get("Telegram Bot", "BotUsername", String.class);
    botToken = config.get("Telegram Bot", "BotToken", String.class);
    chatWhiteList = parseList(
        config.get("Telegram Bot", "ChatWhiteList", String.class));
    debugUsers = parseList(
        config.get("Telegram Bot", "DebugUsers", String.class));
    String language = config.get("Telegram Bot", "DefaultLanguage",
        String.class);
    if (Language.parseFromString(language) != Language.UNKNOWN) {
      String[] strs = language.split("_");
      defaultLocale = new Locale(strs[0], strs[1]);
    } else {
      throw new IllegalArgumentException(
          "Invalid DefaultLanguage. Acceptable values: " + Arrays.toString(
              ArrayUtils.removeElement(Language.values(), Language.UNKNOWN)));
    }
    defaultTimeZone = TimeZone.getTimeZone(
        config.get("Telegram Bot", "DefaultTimeZone", String.class));
    tempDir = config.get("Telegram Bot", "TempDir", String.class);
    if (!new File(tempDir).exists()) {
      throw new IllegalArgumentException(
          "Invalid TempDir. No such directory: " + tempDir);
    }

    // MySQL.
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

    // Graphviz.
    String graphvizPath = config.get("Graphviz", "GraphvizPath", String.class);
    graphVisualizationUtil = new Graphviz(graphvizPath, tempDir);

    // Turing Robot.
    chatBot = new TuringRobot(
        config.get("Turing Robot", "TuringRobotApiKey", String.class));
  }

  // The input is expected to be a comma separated list.
  private Set<Long> parseList(String text) {
    Set<Long> set = new HashSet<>();
    String[] items = text.split(",\\s*");
    for (String item : items) {
      set.add(Long.parseLong(item));
    }
    return set;
  }

  private boolean isChatInWhiteList(long chatId) {
    return chatWhiteList.contains(chatId);
  }

  private void registerHandler(Handler handler) throws Exception {
    String commandName = handler.getCommandName();
    if (handlerMap.put(commandName, handler) != null) {
      throw new Exception(
          "Failed to register handler. Duplicated command name: "
              + commandName);
    }
  }

  private void registerHandlers() throws Exception {
    registerHandler(new RankHandler(this, database));
    registerHandler(new KeywordHandler(this, database, nlpUtil));
    registerHandler(new GoToHandler(this));
    registerHandler(
        new NetworkHandler(this, database, graphVisualizationUtil, tempDir));
    registerHandler(new TimeStatsHandler(this, database, tempDir));
    registerHandler(new EchoHandler(this));
    registerHandler(new HistoryHandler(this, database));
    registerHandler(new ChatBotHandler(this, database, chatBot));
    registerHandler(new OptionHandler(this, database));
    registerHandler(new HelpHandler(this, handlerMap));
  }

  private MessageType handleMessage(Message message)
      throws TelegramApiException, ClassNotFoundException, SQLException {
    MessageType messageType = MessageType.UNKNOWN;
    long chatId = message.getChatId();
    if (message.hasText()) {
      if (parseCommand(chatId, chatId, message.getText(), message)) {
        messageType = MessageType.COMMAND;
      } else {
        messageType = MessageType.TEXT;
      }
    } else if (message.getSticker() != null) {
      messageType = MessageType.STICKER;
    } else if (message.hasDocument()) {
      String mimeType = message.getDocument().getMimeType();
      if (mimeType.equals("video/mp4")) {
        messageType = MessageType.GIF;
      } else if (mimeType.startsWith("image")) {
        messageType = MessageType.IMAGE;
      }
    } else if (message.hasPhoto()) {
      messageType = MessageType.IMAGE;
    } else if (message.getVideo() != null) {
      messageType = MessageType.VIDEO;
    } else if (message.getAudio() != null) {
      messageType = MessageType.AUDIO;
    } else if (message.getVoice() != null) {
      messageType = MessageType.VOICE;
    } else if (message.getPinnedMessage() != null) {
      messageType = MessageType.PINNED_MESSAGE;
    } else if (message.getNewChatTitle() != null) {
      messageType = MessageType.CHAT_TITLE;
    } else if (message.getNewChatPhoto() != null) {
      messageType = MessageType.CHAT_PHOTO;
    }
    return messageType;
  }

  // Returns true if the text starts with / or >.
  private boolean parseCommand(long receiverChatId, long dataSourceChatId,
      String command, Message message)
      throws TelegramApiException, ClassNotFoundException, SQLException {
    String[] arguments = command.split("\\s+");
    boolean isCommand = false;
    boolean isClickableCommand = false;
    // Clickable command. No response if invalid.
    if (command.startsWith(CommandUtil.CLICKABLE_COMMAND_PREFIX)
        && command.length() > 1) {
      command = CommandUtil.clickableToNonClickable(command);
      isClickableCommand = true;
      isCommand = true;
    }
    // Command. Always has response.
    if (command.startsWith(CommandUtil.NON_CLICKABLE_COMMAND_PREFIX)
        && command.length() > 1) {
      isCommand = true;
      arguments = command.split("\\s+");
      String commandName = arguments[0].substring(1);
      arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
      Locale locale = getLocale(dataSourceChatId, database);
      ResourceBundle responseResource = ResourceBundle
          .getBundle(RESPONSE_RESOURCE_BUNDLE, locale);
      if (handlerMap.containsKey(commandName)) {
        handlerMap.get(commandName).handle(receiverChatId, dataSourceChatId,
            arguments, message, locale);
      } else if (commandName.equals("debug")) {
        long senderId = message.getFrom().getId();
        if (debugUsers.contains(senderId)) {
          debug(receiverChatId, message);
        } else {
          sendTextMessage(receiverChatId,
              responseResource.getString("debug.noPermission"));
        }
      } else if (!isClickableCommand) {
        sendTextMessage(receiverChatId,
            responseResource.getString("common.noSuchCommand") + ": "
                + commandName + "\n/help");
      }
    }
    return isCommand;
  }

  // Debug mode. Use the data from other chat as data source.
  // Usage: >debug [chat id] >[command]
  // Example: >debug 123456 >keyword 200
  private void debug(long receiverChatId, Message message)
      throws TelegramApiException, ClassNotFoundException, SQLException {
    String text = message.getText();
    String[] arguments = text.split("\\s+");
    if (arguments.length > 2) {
      long dataSourceChatId = Long.parseLong(arguments[1]);
      String command = text.replaceFirst(
          CommandUtil.NON_CLICKABLE_COMMAND_PREFIX + ".+"
              + CommandUtil.NON_CLICKABLE_COMMAND_PREFIX,
          CommandUtil.NON_CLICKABLE_COMMAND_PREFIX);
      logger.info("[Debug mode] Data source chat id: " + dataSourceChatId
          + "  Command: " + command);
      parseCommand(receiverChatId, dataSourceChatId, command, message);
    } else {
      sendTextMessage(receiverChatId, "Invalid argument.");
    }
  }

  // Saves message and user info.
  private void saveMessage(Message message, MessageType messageType)
      throws ClassNotFoundException, SQLException {
    Long chatId = message.getChatId();
    long timeEpochSeconds = message.getDate();
    User sender = message.getFrom();
    long messageId = message.getMessageId();
    long userId = sender.getId();
    String username = sender.getUserName();
    String firstName = sender.getFirstName();
    String lastName = sender.getLastName();
    long replyToMessageId = 0L; // 0 means no reply.
    long replyToUserId = 0L; // 0 means no reply.
    if (message.getReplyToMessage() != null) {
      replyToMessageId = message.getReplyToMessage().getMessageId();
      replyToUserId = message.getReplyToMessage().getFrom().getId();
    } else if (message.getPinnedMessage() != null) {
      replyToMessageId = message.getPinnedMessage().getMessageId();
      replyToUserId = message.getPinnedMessage().getFrom().getId();
    }
    String content = getMessageContent(message, messageType);
    if (messageType == MessageType.UNKNOWN) {
      logger.info("Message with unknown type: " + message.toString());
    }

    database.openConnection();
    database.addMessage(new com.neoshell.telegram.messageanalysisbot.Message(
        chatId, messageId, timeEpochSeconds, userId, replyToMessageId,
        replyToUserId, content, messageType));
    database.addOrUpdateUser(new com.neoshell.telegram.messageanalysisbot.User(
        userId, username, firstName, lastName));
    database.closeConnection();
  }

  private String getMessageContent(Message message, MessageType messageType) {
    switch (messageType) {
    case TEXT:
    case COMMAND:
      return message.getText();
    case STICKER:
      return message.getSticker().getFileId();
    case GIF:
      return message.getDocument().getFileId();
    case IMAGE:
      return message.getPhoto().get(2).getFileId();
    case VIDEO:
      return message.getVideo().getFileId();
    case AUDIO:
      return message.getAudio().getFileId();
    case VOICE:
      return message.getVoice().getFileId();
    case CHAT_PHOTO:
      return message.getNewChatPhoto().get(2).getFileId();
    case CHAT_TITLE:
      return message.getNewChatTitle();
    case PINNED_MESSAGE:
      String text = message.getPinnedMessage().getText();
      return text != null ? text : "";
    default:
      return "";
    }
  }

  // Retrieves language option according to the chat id.
  private Locale getLocale(long chatId, DatabaseInterface database)
      throws SQLException, ClassNotFoundException {
    database.openConnection();
    String language = database.getOption(chatId, LANGUAGE_OPTION_NAME);
    database.closeConnection();
    Locale locale = defaultLocale;
    if (language != null
        && Language.parseFromString(language) != Language.UNKNOWN) {
      String[] strs = language.split("_");
      locale = new Locale(strs[0], strs[1]);
    }
    return locale;
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
    try {
      TelegramBotsApi telegramBotsApi = new TelegramBotsApi(
          DefaultBotSession.class);
      String configFilePath = args.length >= 1 ? args[0] : DEFAULT_CONFIG_PATH;
      telegramBotsApi.registerBot(new MessageAnalysisBot(configFilePath));
    } catch (TelegramApiException e) {
      logger.severe(ExceptionUtils.getStackTrace(e));
    }
  }

}
