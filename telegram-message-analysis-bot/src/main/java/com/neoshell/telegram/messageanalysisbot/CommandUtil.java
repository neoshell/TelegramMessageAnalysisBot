package com.neoshell.telegram.messageanalysisbot;

public class CommandUtil {

  public static String CLICKABLE_COMMAND_PREFIX = "/";
  public static String NON_CLICKABLE_COMMAND_PREFIX = ">";

  public static String clickableToNonClickable(String command)
      throws IllegalArgumentException {
    if (!command.startsWith(CLICKABLE_COMMAND_PREFIX)) {
      throw new IllegalArgumentException();
    }
    String newCommand = command.split("[@\\s]+")[0];
    return newCommand.replaceAll("_{2,}", "_-").replaceAll("_", " ")
        .replaceFirst(CLICKABLE_COMMAND_PREFIX, NON_CLICKABLE_COMMAND_PREFIX);
  }

  public static String nonClickableToClickable(String command)
      throws IllegalArgumentException {
    if (!command.startsWith(NON_CLICKABLE_COMMAND_PREFIX)) {
      throw new IllegalArgumentException();
    }
    return command.replaceAll("[-\\s]", "_")
        .replaceFirst(NON_CLICKABLE_COMMAND_PREFIX, CLICKABLE_COMMAND_PREFIX);
  }

}
