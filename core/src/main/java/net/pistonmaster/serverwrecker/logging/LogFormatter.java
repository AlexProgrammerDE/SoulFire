/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
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
