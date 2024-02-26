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
package com.soulfiremc.server.viaversion;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JLoggerToSLF4J extends Logger {
  private final org.slf4j.Logger base;

  public JLoggerToSLF4J(org.slf4j.Logger logger) {
    super("logger", null);
    this.base = logger;
  }

  @Override
  public void log(Level level, String msg) {
    this.base.atLevel(toSLF4JLevel(level)).log(msg);
  }

  @Override
  public void log(Level level, String msg, Object param1) {
    this.base.atLevel(toSLF4JLevel(level)).log(msg, param1);
  }

  @Override
  public void log(Level level, String msg, Object... params) {
    log(level, MessageFormat.format(msg, params));
  }

  @Override
  public void log(Level level, String msg, Throwable params) {
    this.base.atLevel(toSLF4JLevel(level)).log(msg, params);
  }

  private org.slf4j.event.Level toSLF4JLevel(Level level) {
    if (level == Level.FINE) {
      return org.slf4j.event.Level.DEBUG;
    } else if (level == Level.WARNING) {
      return org.slf4j.event.Level.WARN;
    } else if (level == Level.SEVERE) {
      return org.slf4j.event.Level.ERROR;
    } else if (level == Level.INFO) {
      return org.slf4j.event.Level.INFO;
    } else {
      return org.slf4j.event.Level.TRACE;
    }
  }
}
