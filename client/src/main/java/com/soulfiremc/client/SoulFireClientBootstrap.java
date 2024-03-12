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
import com.soulfiremc.client.gui.popups.ServerSelectDialog;
import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.grpc.DefaultAuthSystem;
import java.awt.GraphicsEnvironment;
import java.util.List;
import javax.swing.SwingUtilities;
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
    // We may split client and server mixins in the future
    var runHeadless = GraphicsEnvironment.isHeadless() || args.length > 0;

    if (runHeadless) {
      String host;
      int port;
      String jwtToken;
      if (System.getProperty("sf.remoteHost") != null) {
        host = System.getProperty("sf.remoteHost");
        port = Integer.getInteger("sf.remotePort");

        log.info("Using remote server on {}:{}", host, port);
        jwtToken = System.getProperty("sf.remoteJWT");
      } else {
        host = getRPCHost("localhost");
        port = getRandomRPCPort();

        log.info("Starting integrated server on {}:{}", host, port);
        var soulFire =
            new SoulFireServer(host, port, PLUGIN_MANAGER, START_TIME, new DefaultAuthSystem());

        jwtToken = soulFire.generateIntegratedUserJWT();
      }

      var rpcClient = new RPCClient(host, port, jwtToken);
      log.info("Starting CLI");
      var cliManager = new CLIManager(rpcClient, PLUGIN_MANAGER);
      cliManager.initCLI(args);
    } else {
      GUIManager.injectTheme();
      GUIManager.loadGUIProperties();

      SwingUtilities.invokeLater(
          () ->
              new ServerSelectDialog(
                  () -> {
                    var host = getRPCHost("localhost");
                    var port = getRandomRPCPort();

                    log.info("Starting integrated server");
                    var soulFire =
                        new SoulFireServer(
                            host, port, PLUGIN_MANAGER, START_TIME, new DefaultAuthSystem());

                    var jwtToken = soulFire.generateIntegratedUserJWT();
                    var rpcClient = new RPCClient(host, port, jwtToken);

                    log.info("Starting GUI");
                    var guiManager = new GUIManager(rpcClient, PLUGIN_MANAGER);
                    guiManager.initGUI();
                  },
                  remoteServerData -> {
                    var rpcClient =
                        new RPCClient(
                            remoteServerData.serverAddress().host(),
                            remoteServerData.serverAddress().port(),
                            remoteServerData.token());
                    log.info("Starting GUI");
                    var guiManager = new GUIManager(rpcClient, PLUGIN_MANAGER);
                    guiManager.initGUI();
                  }));
    }
  }
}
