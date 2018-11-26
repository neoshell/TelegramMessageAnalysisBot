package com.neoshell.telegram.messageanalysisbot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neoshell.telegram.messageanalysisbot.Message;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.User;

public class MySQLDatabase implements DatabaseInterface {

  private String databaseURL;
  private String user;
  private String password;

  private Connection connection;

  public MySQLDatabase(String databaseURL, String user, String password)
      throws ClassNotFoundException, SQLException {
    this.databaseURL = databaseURL;
    this.user = user;
    this.password = password;
    openConnection();
    createUsersTableIfNotExists();
    createMessagesTableIfNotExists();
    createOptionsTableIfNotExists();
    createWeeklyFreqWordCountTableIfNotExists();
    closeConnection();
  }

  @Override
  public void openConnection() throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.jdbc.Driver");
    connection = DriverManager.getConnection(databaseURL, user, password);
    // Make it compatible with emoji.
    Statement statement = connection.createStatement();
    statement.executeQuery("SET NAMES utf8mb4");
    statement.close();
  }

  @Override
  public void closeConnection() throws SQLException {
    connection.close();
  }

  // Make sure connection is open before calling the following methods.
  
  @Override
  public ResultSet executeQuery(String query) throws SQLException {
    Statement statement = connection.createStatement();
    return statement.executeQuery(query);
  }

  @Override
  public void addMessage(Message message) throws SQLException {
    String query = "INSERT INTO messages"
        + "(chat_id, message_id, epoch_seconds, user_id, reply_to_message_id, "
        + "reply_to_user_id, content, type) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    Long replyToMessageId = message.getReplyToMessageId();
    Long replyToUserId = message.getReplyToUserId();
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, message.getChatId());
    preparedStatement.setLong(2, message.getMessageId());
    preparedStatement.setLong(3, message.getEpochSeconds());
    preparedStatement.setLong(4, message.getUserId());
    if (replyToMessageId != null) {
      preparedStatement.setLong(5, replyToMessageId);
    } else {
      preparedStatement.setNull(5, Types.BIGINT);
    }
    if (replyToUserId != null) {
      preparedStatement.setLong(6, replyToUserId);
    } else {
      preparedStatement.setNull(6, Types.BIGINT);
    }
    preparedStatement.setString(7, message.getContent());
    preparedStatement.setString(8, message.getType().toString());
    preparedStatement.execute();
    preparedStatement.close();
  }

  @Override
  public void addOrUpdateUser(User user) throws SQLException {
    String query = "REPLACE INTO users"
        + "(user_id, username, first_name, last_name) "
        + "VALUES (?, ?, ?, ?);";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, user.getUserId());
    preparedStatement.setString(2, user.getUserName());
    preparedStatement.setString(3, user.getFirstName());
    preparedStatement.setString(4, user.getLastName());
    preparedStatement.execute();
    preparedStatement.close();
  }

  @Override
  public void addOrUpdateWordCount(long chatId, int timeRangeIndex, String word,
      long count) throws SQLException {
    String query = "REPLACE INTO monthly_freq_word_count "
        + "(chat_id, month_index, word, count) "
        + "VALUES (?, ?, ?, ?);";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setInt(2, timeRangeIndex);
    preparedStatement.setString(3, word);
    preparedStatement.setLong(4, count);
    preparedStatement.execute();
    preparedStatement.close();
  }

  @Override
  public void addOrUpdateOptions(long chatId, String optionName,
      String optionValue) throws SQLException {
    String query = "REPLACE INTO options "
        + "(chat_id, option_name, option_value) " 
        + "VALUES (?, ?, ?);";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setString(2, optionName);
    preparedStatement.setString(3, optionValue);
    preparedStatement.execute();
    preparedStatement.close();
  }

  @Override
  public Map<Long, User> getUsers() throws SQLException {
    Map<Long, User> map = new HashMap<>();
    String query = "SELECT * from users;";
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(query);
    while (resultSet.next()) {
      long userId = resultSet.getLong("user_id");
      String username = resultSet.getString("username");
      String firstName = resultSet.getString("first_name");
      String lastName = resultSet.getString("last_name");
      map.put(userId, new User(userId, username, firstName, lastName));
    }
    resultSet.close();
    statement.close();
    return map;
  }

  @Override
  public List<Message> getMessages(long chatId, Collection<MessageType> types,
      long startEpochSeconds, long endEpochSeconds) throws SQLException {
    List<Message> messages = new ArrayList<>();
    String query = "SELECT * FROM messages "
        + "WHERE chat_id=? AND "
        + "type IN (" + commaSeparatedQuestionMarks(types.size()) + ") AND "
        + "epoch_seconds>=? AND epoch_seconds<?;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    int paramIndex = 1;
    preparedStatement.setLong(paramIndex++, chatId);
    for (MessageType type : types) {
      preparedStatement.setString(paramIndex++, type.toString());
    }
    preparedStatement.setLong(paramIndex++, startEpochSeconds);
    preparedStatement.setLong(paramIndex++, endEpochSeconds);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      long messageId = resultSet.getLong("message_id");
      long epochSeconds = resultSet.getLong("epoch_seconds");
      long userId = resultSet.getLong("user_id");
      long replyToMessageId = resultSet.getLong("reply_to_message_id");
      long replyToUserId = resultSet.getLong("reply_to_user_id");
      String content = resultSet.getString("content");
      MessageType type = MessageType
          .parseFromString(resultSet.getString("type"));
      messages.add(new Message(chatId, messageId, epochSeconds, userId,
          replyToMessageId, replyToUserId, content, type));
    }
    resultSet.close();
    preparedStatement.close();
    return messages;
  }

  @Override
  public List<Message> getMessagesSortedByTime(long chatId,
      Collection<MessageType> types, int limit, boolean isOldest,
      boolean isAscending) throws SQLException {
    List<Message> messages = new ArrayList<>();
    String query = "SELECT * FROM ("
        + "SELECT * FROM messages "
        + "WHERE chat_id=? AND "
        + "type IN (" + commaSeparatedQuestionMarks(types.size()) + ") "
        + "ORDER BY epoch_seconds " + (isOldest ? "ASC" : "DESC") + " LIMIT ?"
        + ") AS message_table "
        + "ORDER BY epoch_seconds " + (isAscending ? "ASC" : "DESC") + ";";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    int paramIndex = 1;
    preparedStatement.setLong(paramIndex++, chatId);
    for (MessageType type : types) {
      preparedStatement.setString(paramIndex++, type.toString());
    }
    preparedStatement.setInt(paramIndex++, limit);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      long messageId = resultSet.getLong("message_id");
      long epochSeconds = resultSet.getLong("epoch_seconds");
      long userId = resultSet.getLong("user_id");
      long replyToMessageId = resultSet.getLong("reply_to_message_id");
      long replyToUserId = resultSet.getLong("reply_to_user_id");
      String content = resultSet.getString("content");
      MessageType type = MessageType
          .parseFromString(resultSet.getString("type"));
      messages.add(new Message(chatId, messageId, epochSeconds, userId,
          replyToMessageId, replyToUserId, content, type));
    }
    resultSet.close();
    preparedStatement.close();
    return messages;
  }

  @Override
  public List<Long> getChatIds() throws SQLException {
    List<Long> chatIds = new ArrayList<>();
    String query = "SELECT DISTINCT chat_id FROM messages;";
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(query);
    while (resultSet.next()) {
      long chatId = resultSet.getLong("chat_id");
      chatIds.add(chatId);
    }
    resultSet.close();
    statement.close();
    return chatIds;
  }

  @Override
  public List<Map.Entry<User, Integer>> getRank(long chatId,
      long startEpochSeconds, long endEpochSeconds, String type)
          throws SQLException {
    List<Map.Entry<User, Integer>> rankList = new ArrayList<>();
    String query = "SELECT * FROM ("
        + "SELECT user_id, COUNT(*) AS count FROM messages "
        + "WHERE chat_id=? AND epoch_seconds>=? AND epoch_seconds<? AND type=? "
        + "GROUP BY user_id"
        + ") AS count_table "
        + "INNER JOIN ("
        + "SELECT * FROM users) AS users_table "
        + "ON count_table.user_id=users_table.user_id "
        + "ORDER BY count DESC;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setLong(2, startEpochSeconds);
    preparedStatement.setLong(3, endEpochSeconds);
    preparedStatement.setString(4, type);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      long userId = resultSet.getLong("user_id");
      String username = resultSet.getString("username");
      String firstName = resultSet.getString("first_name");
      String lastName = resultSet.getString("last_name");
      int count = resultSet.getInt("count");
      rankList.add(new AbstractMap.SimpleEntry<User, Integer>(
          new User(userId, username, firstName, lastName), count));
    }
    resultSet.close();
    preparedStatement.close();
    return rankList;
  }

  @Override
  public Map<User, int[]> getTimeDistributionInHour(long chatId,
      long startEpochSeconds, long endEpochSeconds) throws SQLException {
    Map<User, int[]> map = new HashMap<>();
    String query = "SELECT * FROM ("
        + "SELECT user_id, FLOOR(epoch_seconds/3600)%24 AS hour, "
        + "COUNT(*) AS count FROM messages "
        + "WHERE chat_id=? AND epoch_seconds>=? AND epoch_seconds<? "
        + "GROUP BY user_id, hour"
        + ") AS count_table "
        + "INNER JOIN ("
        + "SELECT * FROM users"
        + ") AS users_table "
        + "ON count_table.user_id=users_table.user_id "
        + "ORDER BY count_table.user_id ASC, hour ASC;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setLong(2, startEpochSeconds);
    preparedStatement.setLong(3, endEpochSeconds);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      long userId = resultSet.getLong("user_id");
      String username = resultSet.getString("username");
      String firstName = resultSet.getString("first_name");
      String lastName = resultSet.getString("last_name");
      User user = new User(userId, username, firstName, lastName);
      if (!map.containsKey(user)) {
        map.put(user, new int[24]);
      }
      map.get(user)[resultSet.getInt("hour")] = resultSet.getInt("count");
    }
    resultSet.close();
    preparedStatement.close();
    return map;
  }

  @Override
  public List<Map.Entry<Long, Long>> getReplyList(long chatId,
      long startEpochSeconds, long endEpochSeconds) throws SQLException {
    List<Map.Entry<Long, Long>> replyList = new ArrayList<>();
    String query = "SELECT user_id, reply_to_user_id FROM messages "
        + "WHERE chat_id=? AND epoch_seconds>=? AND epoch_seconds<? "
        + "ORDER BY epoch_seconds ASC";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setLong(2, startEpochSeconds);
    preparedStatement.setLong(3, endEpochSeconds);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      long userId = resultSet.getLong("user_id");
      long replyToUserId = resultSet.getLong("reply_to_user_id");
      replyList
          .add(new AbstractMap.SimpleEntry<Long, Long>(userId, replyToUserId));
    }
    resultSet.close();
    preparedStatement.close();
    return replyList;
  }

  @Override
  public Map<String, Long> getWordCount(long chatId, int startTimeRangeIndex,
      int endTimeRangeIndex, int limit) throws SQLException {
    Map<String, Long> wordFrequencyMap = new HashMap<>();
    String query = "SELECT word, SUM(count) AS total_count "
        + "FROM monthly_freq_word_count "
        + "WHERE chat_id=? AND month_index>=? AND month_index<? "
        + "GROUP BY word "
        + "ORDER BY total_count DESC LIMIT ?;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setInt(2, startTimeRangeIndex);
    preparedStatement.setInt(3, endTimeRangeIndex);
    preparedStatement.setInt(4, limit);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      String word = resultSet.getString("word");
      long count = resultSet.getLong("total_count");
      wordFrequencyMap.put(word, count);
    }
    resultSet.close();
    preparedStatement.close();
    return wordFrequencyMap;
  }

  @Override
  public int getMaxWordCountTimeRangeIndex(long chatId) throws SQLException {
    int monthIndex = 0;
    String query = "SELECT MAX(month_index) AS max_month_index "
        + "FROM monthly_freq_word_count "
        + "WHERE chat_id=?;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      monthIndex = resultSet.getInt("max_month_index");
    }
    resultSet.close();
    preparedStatement.close();
    return monthIndex;
  }

  @Override
  public String getOption(long chatId, String optionName) throws SQLException {
    String optionValue = null;
    String query = "SELECT option_value FROM options "
        + "WHERE chat_id=? AND option_name=?;";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    preparedStatement.setLong(1, chatId);
    preparedStatement.setString(2, optionName);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      optionValue = resultSet.getString("option_value");
    }
    resultSet.close();
    preparedStatement.close();
    return optionValue;
  }

  private void createUsersTableIfNotExists() throws SQLException {
    String query = "CREATE TABLE IF NOT EXISTS users ("
        + "user_id BIGINT PRIMARY KEY,"
        + "username CHAR(32),"
        + "first_name CHAR(32),"
        + "last_name CHAR(32)"
        + ") CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    Statement statement = connection.createStatement();
    statement.execute(query);
    statement.close();
  }

  private void createMessagesTableIfNotExists() throws SQLException {
    String query = "CREATE TABLE IF NOT EXISTS messages ("
        + "chat_id BIGINT NOT NULL,"
        + "message_id BIGINT NOT NULL,"
        + "epoch_seconds BIGINT NOT NULL,"
        + "user_id BIGINT NOT NULL,"
        + "reply_to_message_id BIGINT,"
        + "reply_to_user_id BIGINT,"
        + "content TEXT,"
        + "type CHAR(16) NOT NULL,"
        + "PRIMARY KEY(chat_id, message_id),"
        + "INDEX(epoch_seconds, user_id, type(16))"
        + ") CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    Statement statement = connection.createStatement();
    statement.execute(query);
    statement.close();
  }

  private void createOptionsTableIfNotExists() throws SQLException {
    String query = "CREATE TABLE IF NOT EXISTS options ("
        + "chat_id BIGINT NOT NULL,"
        + "option_name CHAR(32) NOT NULL,"
        + "option_value CHAR(128),"
        + "PRIMARY KEY(chat_id, option_name(32))"
        + ") CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    Statement statement = connection.createStatement();
    statement.execute(query);
    statement.close();
  }

  private void createWeeklyFreqWordCountTableIfNotExists() throws SQLException {
    String query = "CREATE TABLE IF NOT EXISTS monthly_freq_word_count ("
        + "chat_id BIGINT NOT NULL,"
        + "month_index INT NOT NULL,"
        + "word CHAR(32) NOT NULL,"
        + "count BIGINT NOT NULL,"
        + "PRIMARY KEY(chat_id, month_index, word(32))"
        + ") CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    Statement statement = connection.createStatement();
    statement.execute(query);
    statement.close();
  }

  private String commaSeparatedQuestionMarks(int valueSize) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < valueSize; i++) {
      sb.append("?,");
    }
    sb.deleteCharAt(sb.length() - 1); // Remove the last comma.
    return sb.toString();
  }

}
