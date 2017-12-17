package com.neoshell.telegram.messageanalysisbot;

public enum Language {
  UNKNOWN("unknown"), EN_US("en_US"), ZH_CN("zh_CN");

  private String code;

  public static Language parseFromString(String string) {
    switch (string) {
    case "en_US":
      return EN_US;
    case "zh_CN":
      return ZH_CN;
    default:
      return UNKNOWN;
    }
  }

  private Language(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return code;
  }

}
