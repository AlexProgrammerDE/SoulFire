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
package com.soulfiremc.client;

import com.soulfiremc.client.cli.CLIManager;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.client.gui.GUIManager;
import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.grpc.DefaultAuthSystem;
import java.awt.GraphicsEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoulFireClientBootstrap extends SoulFireAbstractBootstrap {
  private SoulFireClientBootstrap() {
    super();
  }

  @SuppressWarnings("unused")
  public static void bootstrap(String[] args, List<ClassLoader> classLoaders) {
    new SoulFireClientBootstrap().internalBootstrap(args, classLoaders);
  }

  @Override
  protected void postMixinMain(String[] args) {
    String host;
    int port;
    String jwtToken;

    if (System.getProperty("soulfire.remoteHost") != null) {
      host = System.getProperty("soulfire.remoteHost");
      port = Integer.getInteger("soulfire.remotePort");

      log.info("Using remote server on {}:{}", host, port);
      jwtToken = System.getProperty("soulfire.remoteJWT");
    } else {
      host = getRPCHost();
      port = getRPCPort();

      log.info("Starting integrated server on {}:{}", host, port);
      var soulFire =
          new SoulFireServer(
              host,
              port,
              SoulFireClientBootstrap.PLUGIN_MANAGER,
              SoulFireClientBootstrap.START_TIME,
              new DefaultAuthSystem());

      jwtToken = soulFire.generateIntegratedUserJWT();
    }

    // We may split client and server mixins in the future
    var runHeadless = GraphicsEnvironment.isHeadless() || args.length > 0;

    var rpcClient = new RPCClient(host, port, jwtToken);
    if (runHeadless) {
      log.info("Starting CLI");
      var cliManager = new CLIManager(rpcClient, PLUGIN_MANAGER);
      cliManager.initCLI(args);
    } else {
      GUIManager.injectTheme();
      GUIManager.loadGUIProperties();

      log.info("Starting GUI");
      var guiManager = new GUIManager(rpcClient, PLUGIN_MANAGER);
      guiManager.initGUI();
    }
  }
}
