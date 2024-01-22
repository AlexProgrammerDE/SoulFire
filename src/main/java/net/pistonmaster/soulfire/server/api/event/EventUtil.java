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

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;

import java.util.List;
import java.util.Map;

public class EventUtil {
    private static final FieldWrapper handlersWrapper = RStream.of(LambdaManager.class).fields().by("handlers");

    public static void runAndAssertChanged(LambdaManager manager, Runnable runnable) {
        var handlers = handlersWrapper.<Map<?, List<?>>>get(manager);
        var initialHandlers = countTotalHandlers(handlers);
        runnable.run();
        var finalHandlers = countTotalHandlers(handlers);
        if (initialHandlers == finalHandlers) {
            throw new IllegalStateException("No handlers changed!");
        }
    }

    private static int countTotalHandlers(Map<?, List<?>> handlers) {
        return handlers.values().stream().mapToInt(List::size).sum();
    }
}
