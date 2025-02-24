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
import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.Plugin;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.structs.ServerAddress;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class SoulFireCLIBootstrap extends SoulFireAbstractBootstrap {
  private SoulFireCLIBootstrap() {
    super();
  }

  @SuppressWarnings("unused")
  public static void bootstrap(String[] args) {
    if (args.length == 0) {
      System.err.println("ERROR: No command line arguments provided, please run with --help to see available options (java -jar ReplaceMe.jar --help)");
      System.exit(1);
      return;
    }

    new SoulFireCLIBootstrap().internalBootstrap(args);
  }

  private void startCLI(ServerAddress address, String jwt, String[] args) {
    var rpcClient =
      new RPCClient(address.host(), address.port(), jwt);

    log.info("Starting CLI");
    var cliManager = new CLIManager(rpcClient, pluginManager);
    cliManager.initCLI(args);
  }

  @Override
  protected void postMixinMain(String[] args) {
    pluginManager.getExtensions(Plugin.class).forEach(SoulFireAPI::registerServerExtension);

    Runnable runIntegratedServer =
      () -> {
        var host = getRPCHost("localhost");
        var port = getRandomRPCPort();

        log.info("Starting integrated server on {}:{}", host, port);
        var soulFire =
          new SoulFireServer(host, port, pluginManager, START_TIME, getBaseDirectory());

        var jwtToken = soulFire.authSystem().generateJWT(soulFire.authSystem().rootUserData());
        startCLI(
          ServerAddress.fromStringAndPort(host, port),
          jwtToken,
          args
        );
      };
    var address = System.getProperty("sf.remoteAddress");
    if (address == null) {
      runIntegratedServer.run();
    } else {
      var jwtToken = System.getProperty("sf.remoteToken");

      Objects.requireNonNull(address, "Remote address must be set");
      Objects.requireNonNull(jwtToken, "Remote token must be set");

      log.info("Using remote server on {}", address);

      startCLI(
        ServerAddress.fromStringDefaultPort(address, PortHelper.SF_DEFAULT_PORT),
        jwtToken,
        args
      );
    }
  }

  @Override
  protected Path getBaseDirectory() {
    return SFPathConstants.INTEGRATED_SERVER_DIRECTORY;
  }
}
