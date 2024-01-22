/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.util;

import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.system.SystemLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class SWLogAppender extends AbstractAppender {
    private final AbstractStringLayout.Serializer formatter = new PatternLayout.SerializerBuilder()
            .setAlwaysWriteExceptions(true)
            .setDisableAnsi(true)
            .setNoConsoleNoAnsi(true)
            .setDefaultPattern("[%d{HH:mm:ss} %level] [%logger{1.*}]: %minecraftFormatting{%msg}%xEx")
            .build();
    private final AbstractStringLayout.Serializer builtInFormatter = new PatternLayout.SerializerBuilder()
            .setAlwaysWriteExceptions(true)
            .setDisableAnsi(true)
            .setNoConsoleNoAnsi(true)
            .setDefaultPattern("[%d{HH:mm:ss} %level] [%logger{1}]: %minecraftFormatting{%msg}%xEx")
            .build();

    public SWLogAppender() {
        super("LogPanelAppender", null, null, false, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        String formatted;
        if (event.getLoggerName().startsWith("net.pistonmaster.soulfire")) {
            formatted = builtInFormatter.toSerializable(event);
        } else {
            formatted = formatter.toSerializable(event);
        }

        if (formatted.isBlank()) {
            return;
        }

        SoulFireAPI.postEvent(new SystemLogEvent(formatted));
    }
}
