package com.neoshell.telegram.messageanalysisbot.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.neoshell.telegram.messageanalysisbot.Message;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.User;

public interface DatabaseInterface {

  /**
   * Opens database connection.
   * 
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public void openConnection() throws ClassNotFoundException, SQLException;

  /**
   * Closes database connection.
   * 
   * @throws SQLException
   */
  public void closeConnection() throws SQLException;

  /**
   * Executes query.
   * 
   * @param query
   * @return
   * @throws SQLException
   */
  public ResultSet executeQuery(String query) throws SQLException;

  /**
   * Adds the message into database.
   * 
   * @param message
   * @throws SQLException
   */
  public void addMessage(Message message) throws SQLException;

  /**
   * Adds the user into database. Updates if it already exists.
   *
   * @param user
   * @throws SQLException
   */
  public void addOrUpdateUser(User user) throws SQLException;

  /**
   * Adds the word count into database. Updates if the key already exists.
   * 
   * @param chatId
   * @param timeRangeIndex
   *          A unique number for identifying the time range.
   * @param word
   * @param count
   * @throws SQLException
   */
  public void addOrUpdateWordCount(long chatId, int timeRangeIndex, String word,
      long count) throws SQLException;

  /**
   * Adds the option into database. Updates if the key already exists.
   * 
   * @param chatId
   * @param optionName
   * @param optionValue
   * @throws SQLException
   */
  public void addOrUpdateOptions(long chatId, String optionName,
      String optionValue) throws SQLException;

  /**
   * Gets the information of all users.
   * 
   * @return A map where the key is user id and the value is the User object.
   * @throws SQLException
   */
  public Map<Long, User> getUsers() throws SQLException;

  /**
   * Gets messages in the given time range.
   * 
   * @param chatId
   * @param type
   * @param startEpochSeconds
   * @param endEpochSeconds
   * @return
   * @throws SQLException
   */
  public List<Message> getMessages(long chatId, Collection<MessageType> type,
      long startEpochSeconds, long endEpochSeconds) throws SQLException;

  /**
   * Gets messages sorted by time.
   * 
   * @param chatId
   * @param type
   * @param limit
   *          The max number of messages retrieved from database.
   * @param isOldest
   *          Set to true if you want to get the oldest messages.
   * @param isAscending
   *          Set to true if you want to sort the results in ascending order of
   *          time.
   * @return
   * @throws SQLException
   */
  public List<Message> getMessagesSortedByTime(long chatId,
      Collection<MessageType> types, int limit, boolean isOldest,
      boolean isAscending) throws SQLException;

  /**
   * Gets the ids of the chat that the bot has ever received messages from.
   * 
   * @return
   * @throws SQLException
   */
  public List<Long> getChatIds() throws SQLException;

  /**
   * Gets the rank based on the number of messages of the given type in the
   * given time range.
   * 
   * @param chatId
   * @param startEpochSeconds
   * @param endEpochSeconds
   * @param type
   * @return A list of map entry, where the key is User and the value is number
   *         of messages.
   * @throws SQLException
   */
  public List<Map.Entry<User, Integer>> getRank(long chatId,
      long startEpochSeconds, long endEpochSeconds, String type)
          throws SQLException;

  /**
   * Gets the reply relationship in the given time range.
   * 
   * @param chatId
   * @param startEpochSeconds
   * @param endEpochSeconds
   * @return A list of map entry, where the key is the id of the user who
   *         replied and the value is the id of the user who was replied.
   * @throws SQLException
   */
  public List<Map.Entry<Long, Long>> getReplyList(long chatId,
      long startEpochSeconds, long endEpochSeconds) throws SQLException;

  /**
   * Gets the total numbers of most frequent words in the given time range.
   * 
   * @param chatId
   * @param startTimeRangeIndex
   * @param endTimeRangeIndex
   * @param limit
   *          The max number of words you want to keep in the result.
   * @return
   * @throws SQLException
   */
  public Map<String, Long> getWordCount(long chatId, int startTimeRangeIndex,
      int endTimeRangeIndex, int limit) throws SQLException;

  /**
   * Gets the max time range index for word count. Since WordCounter runs in
   * incremental mode, the time range index is used for determining the position
   * to continue.
   * 
   * @param chatId
   * @return
   * @throws SQLException
   */
  public int getMaxWordCountTimeRangeIndex(long chatId) throws SQLException;

  /**
   * Gets the option value according to the name.
   * 
   * @param chatId
   * @param optionName
   * @return
   * @throws SQLException
   */
  public String getOption(long chatId, String optionName) throws SQLException;

}
