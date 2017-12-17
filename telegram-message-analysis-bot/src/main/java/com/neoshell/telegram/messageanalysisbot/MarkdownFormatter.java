package com.neoshell.telegram.messageanalysisbot;

public class MarkdownFormatter {

  public static String bold(String str) {
    return "*" + str + "*";
  }

  public static String italic(String str) {
    return "_" + str + "_";
  }

  public static String inlineURL(String text, String url) {
    return "[" + text + "](" + url + ")";
  }

  public static String mentionUser(String text, long userId) {
    return "[" + text + "](tg://user?id=" + userId + ")";
  }

  public static String code(String str) {
    return "`" + str + "`";
  }

  public static String codeBlock(String str) {
    return "```\n" + str + "\n```";
  }

}
