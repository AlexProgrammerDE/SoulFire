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
package net.pistonmaster.soulfire.server.plugins;

import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.util.TickTimer;
import org.slf4j.MDC;

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

            MDC.put("connectionId", connection.connectionId().toString());
            MDC.put("botName", connection.meta().minecraftAccount().username());
            MDC.put("botUuid", connection.meta().minecraftAccount().uniqueId().toString());
            try {
                connection.tick(tickTimer.ticks, tickTimer.partialTicks);
            } catch (Throwable t) {
                connection.logger().error("Exception ticking bot", t);
            }
            MDC.clear();
        }, 0, 50, TimeUnit.MILLISECONDS); // 20 TPS
    }

    @Override
    public void onLoad() {
        PluginHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, BotTicker::onConnectionInit);
    }
}
