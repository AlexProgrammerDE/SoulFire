/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.api.event;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.IExceptionHandler;

@Slf4j
public final class EventExceptionHandler implements IExceptionHandler {
  public static final EventExceptionHandler INSTANCE = new EventExceptionHandler();

  @Override
  public void handle(AHandler handler, Object event, Throwable t) {
    log.error(
      "Exception while handling event {} in handler {}",
      event.getClass().getName(),
      handler.getClass().getName(),
      t);
  }
}
