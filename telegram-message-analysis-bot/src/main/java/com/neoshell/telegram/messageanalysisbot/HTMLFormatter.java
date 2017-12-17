package com.neoshell.telegram.messageanalysisbot;

public class HTMLFormatter {

  public static String bold(String str) {
    return "<b>" + str + "</b>";
  }

  public static String italic(String str) {
    return "<i>" + str + "</i>";
  }

  public static String inlineURL(String text, String url) {
    return "<a href=\"" + url + "\">" + text + "</a>";
  }

  public static String mentionUser(String text, long userId) {
    return "<a href=\"tg://user?id=" + userId + "\">" + text + "</a>";
  }

  public static String code(String str) {
    return "<code>" + str + "</code>";
  }

  public static String codeBlock(String str) {
    return "<pre>" + str + "</pre>";
  }

}
