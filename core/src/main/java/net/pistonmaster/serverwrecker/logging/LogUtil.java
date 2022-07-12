package net.pistonmaster.serverwrecker.logging;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
    public static void setLevel(String loggerName, Level level) {
        setLevel(LoggerFactory.getLogger(loggerName), level);
    }

    public static void setLevel(Logger logger, Level level) {
        ((ch.qos.logback.classic.Logger) logger).setLevel(level);
    }
}
