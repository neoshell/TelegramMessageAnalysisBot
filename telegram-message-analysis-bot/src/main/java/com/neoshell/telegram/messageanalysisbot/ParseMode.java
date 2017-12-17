package com.neoshell.telegram.messageanalysisbot;

public enum ParseMode {
  NULL(null), MARKDOWN("Markdown"), HTML("HTML");

  private String mode;

  private ParseMode(String mode) {
    this.mode = mode;
  }

  @Override
  public String toString() {
    return mode;
  }

}
