package com.neoshell.telegram.messageanalysisbot.handler;

public class ReplyStats {

  private long userId;
  private long replyToUserId;
  private boolean isExplicitReply;
  private double score;

  public ReplyStats(long userId, long replyToUserId, boolean isExplicitReply,
      double score) {
    super();
    this.userId = userId;
    this.replyToUserId = replyToUserId;
    this.isExplicitReply = isExplicitReply;
    this.score = score;
  }

  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  public long getReplyToUserId() {
    return replyToUserId;
  }

  public void setReplyToUserId(long replyToUserId) {
    this.replyToUserId = replyToUserId;
  }

  public boolean isExplicitReply() {
    return isExplicitReply;
  }

  public void setExplicitReply(boolean isExplicitReply) {
    this.isExplicitReply = isExplicitReply;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isExplicitReply ? 1231 : 1237);
    result = prime * result + (int) (replyToUserId ^ (replyToUserId >>> 32));
    long temp;
    temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + (int) (userId ^ (userId >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ReplyStats other = (ReplyStats) obj;
    if (isExplicitReply != other.isExplicitReply)
      return false;
    if (replyToUserId != other.replyToUserId)
      return false;
    if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
      return false;
    if (userId != other.userId)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ReplyStats [userId=" + userId + ", replyToUserId=" + replyToUserId
        + ", isExplicitReply=" + isExplicitReply + ", score=" + score + "]";
  }

}
