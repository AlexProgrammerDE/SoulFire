package net.pistonmaster.serverwrecker.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class LogFormatter {
    public static final char COLOR_CHAR = '\u00A7';
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]");

    //displays the hour and am/pm
    private final DateFormat dateFormat = new SimpleDateFormat("h:mm a");
    private final Date date = new Date();

    public String format(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();

        date.setTime(event.getTimeStamp());
        builder.append(dateFormat.format(date)).append(' ');
        builder.append(event.getLevel()).append(' ');
        builder.append('[').append(event.getLoggerName()).append(']').append(' ');
        builder.append(formatMessage(event));
        builder.append("\n");
        return builder.toString();
    }

    public String formatMessage(ILoggingEvent record) {
        String simpleFormattedMessage = record.getFormattedMessage();

        simpleFormattedMessage = STRIP_COLOR_PATTERN.matcher(simpleFormattedMessage).replaceAll("");

        return simpleFormattedMessage;
    }
}
