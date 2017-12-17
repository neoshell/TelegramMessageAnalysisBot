package com.neoshell.telegram.messageanalysisbot;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CustomizedLogger {

  private static final String LOG_DIR_NAME = "log";
  private static final int LOG_LIMIT = 1000000; // 1MB
  private static final int LOG_COUNT = 10;
  private static final boolean LOG_APPEND = true;

  public static Logger getLogger(Class<?> c, String fileNamePattern)
      throws URISyntaxException, SecurityException, IOException {
    String classDir = getClassDir(c);
    Logger logger = Logger.getLogger(c.getName());
    if (logger.getHandlers().length == 0) {
      File logDir = new File(classDir + "/" + LOG_DIR_NAME);
      if (!logDir.exists()) {
        logDir.mkdirs();
      }
      FileHandler fileHandler = new FileHandler(
          classDir + "/" + LOG_DIR_NAME + "/" + fileNamePattern, LOG_LIMIT,
          LOG_COUNT, LOG_APPEND);
      fileHandler.setLevel(Level.INFO);
      fileHandler.setFormatter(new SimpleFormatter());
      logger.addHandler(fileHandler);
    }
    return logger;
  }

  private static String getClassDir(Class<?> c) throws URISyntaxException {
    boolean isWindows = System.getProperty("os.name").contains("indow");
    String classDir = c.getProtectionDomain().getCodeSource().getLocation()
        .toURI().getPath();
    classDir = isWindows ? classDir.substring(1) : classDir;
    File classDirFile = new File(classDir);
    if (!classDirFile.isDirectory()) {
      classDir = classDirFile.getParent();
    }
    return classDir;
  }

}
