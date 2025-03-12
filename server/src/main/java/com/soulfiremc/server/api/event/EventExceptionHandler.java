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
package com.soulfiremc.server.api.event;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.IExceptionHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventExceptionHandler implements IExceptionHandler {
  public static final EventExceptionHandler INSTANCE = new EventExceptionHandler();
  private static final Logger log = LoggerFactory.getLogger("SoulFire");

  @Override
  public void handle(@NonNull AHandler handler, @NonNull Object event, @NonNull Throwable t) {
    log.error(
      "Exception while handling event {} in handler {}",
      event.getClass().getName(),
      handler.getClass().getName(),
      t);
  }
}
