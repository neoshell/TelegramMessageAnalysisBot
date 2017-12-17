package com.neoshell.telegram.messageanalysisbot.handler;

public class ChatBotReply {

  // TODO: add more fields (e.g. image).
  private String text;

  public ChatBotReply(String text) {
    super();
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}
