package com.neoshell.telegram.messageanalysisbot;

public enum MessageType {
  UNKNOWN("unknown"), TEXT("text"), STICKER("sticker"), GIF("gif"), IMAGE(
      "image"), VIDEO("video"), AUDIO("audio"), VOICE("voice"), CHAT_TITLE(
          "chat_title"), CHAT_PHOTO("chat_photo"), COMMAND(
              "command"), PINNED_MESSAGE("pinned_message");

  private String type;

  public static MessageType parseFromString(String string) {
    switch (string) {
    case "text":
      return TEXT;
    case "sticker":
      return STICKER;
    case "gif":
      return GIF;
    case "image":
      return IMAGE;
    case "video":
      return VIDEO;
    case "audio":
      return AUDIO;
    case "voice":
      return VOICE;
    case "chat_title":
      return CHAT_TITLE;
    case "chat_photo":
      return CHAT_PHOTO;
    case "command":
      return COMMAND;
    case "pinned_message":
      return PINNED_MESSAGE;
    default:
      return UNKNOWN;
    }
  }

  private MessageType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }
}
