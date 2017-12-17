package com.neoshell.telegram.messageanalysisbot;

import static org.junit.Assert.*;

import org.junit.Test;

public class CommandUtilTest {

  @Test
  public void clickableToNonClickable() {
    String clickableCommand = "/goto___m_123456@MyBot";
    String result = CommandUtil.clickableToNonClickable(clickableCommand);
    String expectedResult = ">goto -m 123456";
    assertEquals(expectedResult, result);
  }

  @Test
  public void nonClickableToClickable() {
    String nonClickableCommand = ">goto -m 123456";
    String result = CommandUtil.nonClickableToClickable(nonClickableCommand);
    String expectedResult = "/goto__m_123456";
    assertEquals(expectedResult, result);
  }

}
