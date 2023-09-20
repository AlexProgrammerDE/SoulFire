/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.addons;

import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.util.TickTimer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotTicker implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
        AddonHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, this::onConnectionInit);
    }

    public void onConnectionInit(BotConnectionInitEvent event) {
        startTicker(event.connection(),
                event.connection().executorManager().newScheduledExecutorService("Tick"),
                new TickTimer(20));
    }

    private void startTicker(BotConnection connection, ScheduledExecutorService executor,
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
}
