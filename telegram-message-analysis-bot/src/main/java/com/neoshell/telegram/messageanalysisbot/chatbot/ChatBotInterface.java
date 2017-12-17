package com.neoshell.telegram.messageanalysisbot.chatbot;

import com.neoshell.telegram.messageanalysisbot.handler.ChatBotReply;

public interface ChatBotInterface {

  /**
   * @param userId
   * @param text
   * @return
   * @throws Exception
   */
  public ChatBotReply chat(String userId, String text) throws Exception;

}
