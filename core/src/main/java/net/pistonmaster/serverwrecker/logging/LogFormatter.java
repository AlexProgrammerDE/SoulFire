package net.pistonmaster.serverwrecker.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class LogFormatter extends Formatter {
    public static final char COLOR_CHAR = '\u00A7';
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]");

    //displays the hour and am/pm
    private final DateFormat dateFormat = new SimpleDateFormat("h:mm a");
    private final Date date = new Date();

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        date.setTime(record.getMillis());
        builder.append(dateFormat.format(date)).append(' ');
        builder.append(record.getLevel()).append(' ');
        builder.append('[').append(record.getLoggerName()).append(']').append(' ');
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public String formatMessage(LogRecord record) {
        String simpleFormattedMessage = super.formatMessage(record);

        simpleFormattedMessage = STRIP_COLOR_PATTERN.matcher(simpleFormattedMessage).replaceAll("");

        return simpleFormattedMessage;
    }
}
