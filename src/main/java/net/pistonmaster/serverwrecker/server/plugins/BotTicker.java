/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.server.plugins;

import net.pistonmaster.serverwrecker.server.api.PluginHelper;
import net.pistonmaster.serverwrecker.server.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.serverwrecker.server.protocol.BotConnection;
import net.pistonmaster.serverwrecker.server.util.TickTimer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotTicker implements InternalExtension {
    public static void onConnectionInit(BotConnectionInitEvent event) {
        var connection = event.connection();
        startTicker(connection,
                connection.executorManager().newScheduledExecutorService(connection, "Tick"),
                new TickTimer(20));
    }

    private static void startTicker(BotConnection connection, ScheduledExecutorService executor,
                                    TickTimer tickTimer) {
        executor.scheduleWithFixedDelay(() -> {
            tickTimer.advanceTime();

            try {
                connection.tick(tickTimer.ticks, tickTimer.partialTicks);
            } catch (Throwable t) {
                connection.logger().error("Exception ticking bot", t);
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // 20 TPS
    }

    @Override
    public void onLoad() {
        PluginHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, BotTicker::onConnectionInit);
    }
}
