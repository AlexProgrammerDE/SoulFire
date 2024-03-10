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
package com.soulfiremc.dedicated;

import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.SoulFireServer;
import java.util.concurrent.Executors;

public class SoulFireDedicatedLoader {
  private SoulFireDedicatedLoader() {}

  public static void runDedicated(String host, int port) {
    var soulFire =
        new SoulFireServer(
            host,
            port,
            SoulFireDedicatedBootstrap.PLUGIN_MANAGER,
            SoulFireDedicatedBootstrap.START_TIME);

    SFDedicatedTerminalConsole.setupTerminalConsole(
        Executors.newSingleThreadExecutor(),
        soulFire.shutdownManager(),
        soulFire.injector().getSingleton(ServerCommandManager.class));
  }
}
