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

import com.soulfiremc.brigadier.GenericTerminalConsole;
import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.grpc.DefaultAuthSystem;
import com.soulfiremc.util.PortHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoulFireDedicatedBootstrap extends SoulFireAbstractBootstrap {
  private SoulFireDedicatedBootstrap() {
    super();
  }

  @SuppressWarnings("unused")
  public static void bootstrap(String[] args, List<ClassLoader> classLoaders) {
    new SoulFireDedicatedBootstrap().internalBootstrap(args, classLoaders);
  }

  @Override
  protected void postMixinMain(String[] args) {
    var host = getRPCHost("0.0.0.0");
    var port = getRPCPort(PortHelper.SF_DEFAULT_PORT);

    log.info("Starting dedicated server on {}:{}", host, port);

    GenericTerminalConsole.setupStreams();
    var soulFire =
      new SoulFireServer(host, port, PLUGIN_MANAGER, START_TIME, new DefaultAuthSystem());

    log.info("Tip: To generate a new access token, use the command: 'generate-token'");

    new GenericTerminalConsole(
      soulFire.shutdownManager(),
      soulFire.injector().getSingleton(ServerCommandManager.class))
      .start();

    soulFire.shutdownManager().awaitShutdown();
  }
}
