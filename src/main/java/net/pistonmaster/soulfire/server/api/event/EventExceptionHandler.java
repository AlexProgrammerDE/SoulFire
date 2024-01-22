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
package net.pistonmaster.soulfire.server.api.event;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.IExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventExceptionHandler implements IExceptionHandler {
    public static final EventExceptionHandler INSTANCE = new EventExceptionHandler();
    private static final Logger log = LoggerFactory.getLogger("SoulFire");

    @Override
    public void handle(@NotNull AHandler handler, @NotNull Object event, @NotNull Throwable t) {
        log.error("Exception while handling event " + event.getClass().getName() + " in handler " + handler.getClass().getName(), t);
    }
}
