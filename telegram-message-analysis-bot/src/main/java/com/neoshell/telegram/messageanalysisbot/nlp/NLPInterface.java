package com.neoshell.telegram.messageanalysisbot.nlp;

import java.util.List;
import java.util.Map;

import com.neoshell.telegram.messageanalysisbot.Message;
import com.neoshell.telegram.messageanalysisbot.handler.KeywordInfo;

public interface NLPInterface {

  /**
   * Initializes the instance.
   * 
   * @throws Exception
   */
  public void initialize() throws Exception;

  /**
   * Cleans up and turns off the instance.
   * 
   * @throws Exception
   */
  public void shutdown() throws Exception;

  /**
   * Merges related messages and computes keywords. Non-text messages will be
   * skipped.
   * 
   * @param messages
   * @param globalWordCount
   *          Used for computing keyword score.
   * @return
   */
  public List<KeywordInfo> computeKeywords(List<Message> messages,
      Map<String, Long> globalWordCount);

  /**
   * Given a list of texts, counts the occurrence of each word.
   * 
   * @param texts
   * @param countStopWords
   *          Set to true if you want to count stop words as well.
   * @param limit
   *          The max number of words you want to keep in the result.
   * @return A list of map entry, where the key is word and the value is count.
   *         Sorted by count in descending order.
   */
  public List<Map.Entry<String, Long>> countWords(List<String> texts,
      boolean countStopWords, int limit);

}
