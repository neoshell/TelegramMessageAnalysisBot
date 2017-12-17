package com.neoshell.telegram.messageanalysisbot.handler;

import java.util.List;

public class KeywordInfo {

  private long startTimeSeconds;
  private long endTimeSeconds;
  private long firstMessageId;
  List<String> keywords;

  public KeywordInfo(long startTimeSeconds, long endTimeSeconds,
      long firstMessageId, List<String> keywords) {
    super();
    this.startTimeSeconds = startTimeSeconds;
    this.endTimeSeconds = endTimeSeconds;
    this.firstMessageId = firstMessageId;
    this.keywords = keywords;
  }

  public long getStartTimeSeconds() {
    return startTimeSeconds;
  }

  public void setStartTimeSeconds(long startTimeSeconds) {
    this.startTimeSeconds = startTimeSeconds;
  }

  public long getEndTimeSeconds() {
    return endTimeSeconds;
  }

  public void setEndTimeSeconds(long endTimeSeconds) {
    this.endTimeSeconds = endTimeSeconds;
  }

  public long getFirstMessageId() {
    return firstMessageId;
  }

  public void setFirstMessageId(long firstMessageId) {
    this.firstMessageId = firstMessageId;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

}
