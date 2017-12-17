package com.neoshell.telegram.messageanalysisbot;

public class Message {

  private Long chatId;
  private Long messageId;
  private Long epochSeconds;
  private Long userId;
  private Long replyToMessageId;
  private Long replyToUserId;
  private String content;
  private MessageType type;

  public Message(Long chatId, Long messageId, Long epochSeconds, Long userId,
      Long replyToMessageId, Long replyToUserId, String content,
      MessageType type) {
    super();
    this.chatId = chatId;
    this.messageId = messageId;
    this.epochSeconds = epochSeconds;
    this.userId = userId;
    this.replyToMessageId = replyToMessageId;
    this.replyToUserId = replyToUserId;
    this.content = content;
    this.type = type;
  }

  public Long getChatId() {
    return chatId;
  }

  public void setChatId(Long chatId) {
    this.chatId = chatId;
  }

  public Long getMessageId() {
    return messageId;
  }

  public void setMessageId(Long messageId) {
    this.messageId = messageId;
  }

  public Long getEpochSeconds() {
    return epochSeconds;
  }

  public void setEpochSeconds(Long epochSeconds) {
    this.epochSeconds = epochSeconds;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getReplyToMessageId() {
    return replyToMessageId;
  }

  public void setReplyToMessageId(Long replyToMessageId) {
    this.replyToMessageId = replyToMessageId;
  }

  public Long getReplyToUserId() {
    return replyToUserId;
  }

  public void setReplyToUserId(Long replyToUserId) {
    this.replyToUserId = replyToUserId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((chatId == null) ? 0 : chatId.hashCode());
    result = prime * result + ((content == null) ? 0 : content.hashCode());
    result = prime * result
        + ((epochSeconds == null) ? 0 : epochSeconds.hashCode());
    result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
    result = prime * result
        + ((replyToMessageId == null) ? 0 : replyToMessageId.hashCode());
    result = prime * result
        + ((replyToUserId == null) ? 0 : replyToUserId.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
    Message other = (Message) obj;
    if (chatId == null) {
      if (other.chatId != null)
        return false;
    } else if (!chatId.equals(other.chatId))
      return false;
    if (content == null) {
      if (other.content != null)
        return false;
    } else if (!content.equals(other.content))
      return false;
    if (epochSeconds == null) {
      if (other.epochSeconds != null)
        return false;
    } else if (!epochSeconds.equals(other.epochSeconds))
      return false;
    if (messageId == null) {
      if (other.messageId != null)
        return false;
    } else if (!messageId.equals(other.messageId))
      return false;
    if (replyToMessageId == null) {
      if (other.replyToMessageId != null)
        return false;
    } else if (!replyToMessageId.equals(other.replyToMessageId))
      return false;
    if (replyToUserId == null) {
      if (other.replyToUserId != null)
        return false;
    } else if (!replyToUserId.equals(other.replyToUserId))
      return false;
    if (userId == null) {
      if (other.userId != null)
        return false;
    } else if (!userId.equals(other.userId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Message [chatId=" + chatId + ", messageId=" + messageId
        + ", epochSeconds=" + epochSeconds + ", userId=" + userId
        + ", replyToMessageId=" + replyToMessageId + ", replyToUserId="
        + replyToUserId + ", content=" + content + ", type=" + type.toString()
        + "]";
  }

}
