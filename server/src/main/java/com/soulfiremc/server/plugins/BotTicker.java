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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.event.attack.BotConnectionInitEvent;
import com.soulfiremc.server.util.TickTimer;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;

public class BotTicker implements InternalPlugin {
  public static void onConnectionInit(BotConnectionInitEvent event) {
    var connection = event.connection();
    var tickTimer = new TickTimer(20);
    connection.scheduler().scheduleWithFixedDelay(
      () -> {
        tickTimer.advanceTime();

        MDC.put("connectionId", connection.connectionId().toString());
        MDC.put("botName", connection.accountName());
        MDC.put("botUuid", connection.accountProfileId().toString());
        try {
          connection.tick(tickTimer.ticks);
        } catch (Throwable t) {
          connection.logger().error("Exception ticking bot", t);
        }
        MDC.clear();
      },
      0,
      50,
      TimeUnit.MILLISECONDS); // 20 TPS
  }

  @Override
  public void onLoad() {
    PluginHelper.registerAttackEventConsumer(
      BotConnectionInitEvent.class, BotTicker::onConnectionInit);
  }
}
