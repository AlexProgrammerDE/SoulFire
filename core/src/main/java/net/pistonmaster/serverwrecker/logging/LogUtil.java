/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
