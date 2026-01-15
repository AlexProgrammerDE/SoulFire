/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.bootstrap.client;

import com.google.common.net.HostAndPort;
import com.soulfiremc.bootstrap.SoulFireAbstractBootstrap;
import com.soulfiremc.bootstrap.client.cli.CLIManager;
import com.soulfiremc.bootstrap.client.grpc.RPCClient;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.RPCConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public final class SoulFireCLIBootstrap extends SoulFireAbstractBootstrap {
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

  private void startCLI(String host, int port, String jwt, String[] args) {
    var rpcClient = new RPCClient(host, port, jwt);

    log.info("Starting CLI");
    var cliManager = new CLIManager(rpcClient);
    cliManager.initCLI(args);
  }

  @Override
  protected void postMixinMain(String[] args) {
    Runnable runIntegratedServer =
      () -> {
        var host = SoulFireAbstractBootstrap.getRPCHost("localhost");
        var port = SoulFireAbstractBootstrap.getRandomRPCPort();

        log.info("Starting integrated server on {}:{}", host, port);
        var soulFire =
          new SoulFireServer(host, port, SoulFireAbstractBootstrap.START_TIME);

        var jwtToken = soulFire.authSystem().generateJWT(
          soulFire.authSystem().rootUserData(),
          RPCConstants.API_AUDIENCE
        );
        startCLI(
          host,
          port,
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

      var hostAndPort = HostAndPort.fromString(address).withDefaultPort(PortHelper.SF_DEFAULT_PORT);
      startCLI(
        hostAndPort.getHost(),
        hostAndPort.getPort(),
        jwtToken,
        args
      );
    }
  }
}
