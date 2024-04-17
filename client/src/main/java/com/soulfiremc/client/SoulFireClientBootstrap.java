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
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ServerAddress;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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

    Consumer<RemoteServerData> remoteServerConsumer =
      remoteServerData -> {
        var rpcClient =
          new RPCClient(
            remoteServerData.serverAddress().host(),
            remoteServerData.serverAddress().port(),
            remoteServerData.token());

        if (runHeadless) {
          log.info("Starting CLI");
          var cliManager = new CLIManager(rpcClient, pluginManager);
          cliManager.initCLI(args);
        } else {
          log.info("Starting GUI");
          var guiManager = new GUIManager(rpcClient, pluginManager);
          guiManager.initGUI();
        }
      };
    Runnable runIntegratedServer =
      () -> {
        var host = getRPCHost("localhost");
        var port = getRandomRPCPort();

        log.info("Starting integrated server on {}:{}", host, port);
        var soulFire =
          new SoulFireServer(host, port, pluginManager, START_TIME, new DefaultAuthSystem(), getBaseDirectory());

        var jwtToken = soulFire.generateIntegratedUserJWT();
        remoteServerConsumer.accept(
          new RemoteServerData(ServerAddress.fromStringAndPort(host, port), jwtToken));
      };
    if (runHeadless) {
      var address = System.getProperty("sf.remoteAddress");
      if (address == null) {
        runIntegratedServer.run();
      } else {
        var token = System.getProperty("sf.remoteToken");

        Objects.requireNonNull(address, "Remote address must be set");
        Objects.requireNonNull(token, "Remote token must be set");

        log.info("Using remote server on {}", address);

        remoteServerConsumer.accept(
          new RemoteServerData(ServerAddress.fromStringDefaultPort(
            address, PortHelper.SF_DEFAULT_PORT), token));
      }
    } else {
      GUIManager.loadGUIProperties();
      GUIManager.injectTheme();

      if (Boolean.getBoolean("sf.disableServerSelect")) {
        runIntegratedServer.run();
      } else {
        SwingUtilities.invokeLater(
          () -> new ServerSelectDialog(runIntegratedServer, remoteServerConsumer));
      }
    }
  }

  @Override
  protected Path getBaseDirectory() {
    return SFPathConstants.INTEGRATED_SERVER_DIRECTORY;
  }
}
