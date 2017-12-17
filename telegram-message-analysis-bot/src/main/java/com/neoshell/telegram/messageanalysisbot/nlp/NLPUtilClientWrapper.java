package com.neoshell.telegram.messageanalysisbot.nlp;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.neoshell.nlp.client.NLPUtilClient;
import com.neoshell.nlp.core.NLPContext;
import com.neoshell.nlp.core.WordInfo;
import com.neoshell.nlp.messaging.Conversation;
import com.neoshell.nlp.messaging.Message;
import com.neoshell.nlp.messaging.MessageAnalysisContext;
import com.neoshell.telegram.messageanalysisbot.MessageType;
import com.neoshell.telegram.messageanalysisbot.handler.KeywordInfo;

public class NLPUtilClientWrapper implements NLPInterface {

  // TODO: load from config.
  private static final int DEFAULT_KEYWORDS_TIME_BUCKET_SECONDS = 600;
  // If the number of messages in a time bucket is less than this number, this
  // bucket is considered unimportant and will be dropped.
  private static final int DEFAULT_MIN_NUM_MESSAGES_PER_TIME_BUCKET = 15;
  private static final int DEFAULT_COMMON_KEYWORD_THRESHOLD = 1;
  private static final int DEFAULT_KEYWORDS_LIMIT = 10;

  private String nlpUtilServerHost;
  private int nlpUtilServerPort;
  private NLPUtilClient nlpUtilClient;

  public NLPUtilClientWrapper(String nlpUtilServerHost, int nlpUtilServerPort) {
    this.nlpUtilServerHost = nlpUtilServerHost;
    this.nlpUtilServerPort = nlpUtilServerPort;
  }

  @Override
  public void initialize() throws Exception {
    nlpUtilClient = new NLPUtilClient(nlpUtilServerHost, nlpUtilServerPort);
  }

  @Override
  public void shutdown() throws Exception {
    nlpUtilClient.shutdown();
  }

  @Override
  public List<KeywordInfo> computeKeywords(
      List<com.neoshell.telegram.messageanalysisbot.Message> messages,
      Map<String, Long> globalWordCount) {
    NLPContext nlpContext = nlpUtilClient.generateNLPContext(globalWordCount);
    MessageAnalysisContext messageAnalysisContext = MessageAnalysisContext
        .newBuilder().setNlpContext(nlpContext)
        .setTimeBucketSeconds(DEFAULT_KEYWORDS_TIME_BUCKET_SECONDS)
        .setCommonKeywordThreshold(DEFAULT_COMMON_KEYWORD_THRESHOLD)
        .setKeywordLimit(DEFAULT_KEYWORDS_LIMIT)
        .setMinMessagesPerConversation(DEFAULT_MIN_NUM_MESSAGES_PER_TIME_BUCKET)
        .build();
    List<Message> nlpMessages = new ArrayList<>();
    for (com.neoshell.telegram.messageanalysisbot.Message m : messages) {
      if (m.getType() == MessageType.TEXT) {
        Message nlpMessage = Message.newBuilder().setId(m.getMessageId())
            .setTimestampSeconds(m.getEpochSeconds()).setContent(m.getContent())
            .build();
        nlpMessages.add(nlpMessage);
      }
    }
    List<Conversation> conversations = nlpUtilClient
        .mergeMessagesAndComputeKeywords(nlpMessages, messageAnalysisContext);
    List<KeywordInfo> keywordInfoList = new ArrayList<>();
    for (Conversation conversation : conversations) {
      if (conversation.getMessageCount() == 0) {
        continue;
      }
      List<String> keywords = new ArrayList<>();
      for (WordInfo wordInfo : conversation.getKeywordList()) {
        keywords.add(wordInfo.getWord());
      }
      KeywordInfo keywordInfo = new KeywordInfo(
          conversation.getStartTimestampSeconds(),
          conversation.getEndTimestampSeconds(),
          conversation.getMessage(0).getId(), keywords);
      keywordInfoList.add(keywordInfo);
    }
    return keywordInfoList;
  }

  @Override
  public List<Entry<String, Long>> countWords(List<String> texts,
      boolean countStopWords, int limit) {
    List<WordInfo> wordInfoList = nlpUtilClient.countWords(texts,
        countStopWords, limit);
    List<Map.Entry<String, Long>> wordCount = new ArrayList<>();
    for (WordInfo wordInfo : wordInfoList) {
      wordCount.add(new AbstractMap.SimpleEntry<String, Long>(
          wordInfo.getWord(), wordInfo.getCount()));
    }
    return wordCount;
  }

}
