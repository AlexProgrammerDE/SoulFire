package com.github.games647.lambdaattack.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

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
}
