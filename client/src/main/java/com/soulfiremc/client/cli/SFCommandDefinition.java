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
package com.soulfiremc.client.cli;

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.console.GenericTerminalConsole;
import com.soulfiremc.grpc.generated.InstanceUpdateConfigRequest;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.proxy.ProxyType;
import com.soulfiremc.server.util.SFPathConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@Getter
@RequiredArgsConstructor
@Command(
  name = "soulfire",
  mixinStandardHelpOptions = true,
  version = "SoulFire v" + BuildData.VERSION,
  showDefaultValues = true,
  description = BuildData.DESCRIPTION,
  sortOptions = false,
  showAtFileInUsageHelp = true)
public class SFCommandDefinition implements Callable<Integer> {
  private final CLIManager cliManager;
  @Setter
  private CommandLine commandLine;

  @Option(
    names = {"-s", "--start"},
    description = "Whether to start the attack automatically")
  private boolean start;

  @Option(
    names = {"--account-file"},
    description = "File to load accounts from")
  private Path accountFile;

  @Option(
    names = {"--account-type"},
    description = "Type of accounts in the account file")
  private AuthType authType;

  @Option(
    names = {"--proxy-file"},
    description = "File to load proxies from")
  private Path proxyFile;

  @Option(
    names = {"--proxy-type"},
    description = "Type of proxies in the proxy file")
  private ProxyType proxyType;

  @Option(
    names = {"--generate-flags"},
    description = "Create a list of flags",
    hidden = true)
  private boolean generateFlags;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public Integer call() {
    if (generateFlags) {
      commandLine
        .getCommandSpec()
        .options()
        .forEach(
          option -> {
            if (option.hidden()) {
              return;
            }

            var name =
              Arrays.stream(option.names())
                .map("`%s`"::formatted)
                .collect(Collectors.joining(", "))
                .replace("|", "\\|");
            var defaultValue =
              option.defaultValueString() == null
                ? ""
                : "`%s`"
                .formatted(option.defaultValueString())
                .replace("|", "\\|")
                .replace("\n", " ");
            if (defaultValue.length() > 50) {
              defaultValue = defaultValue.substring(0, 50) + "...`";
            }

            var description =
              option.description() == null
                ? ""
                : String.join(", ", option.description())
                .replace("|", "\\|");
            System.out.printf("| %s | %s | %s |%n", name, defaultValue, description);
          });
      cliManager.shutdown();
      return 0;
    }

    // Delayed to here, so help and version do not get cut off
    GenericTerminalConsole.setupStreams();

    cliManager.clientSettingsManager().commandDefinition(this);

    cliManager.rpcClient().instanceStubBlocking().updateInstanceConfig(InstanceUpdateConfigRequest.newBuilder()
      .setId(cliManager.cliInstanceId().toString())
      .setConfig(cliManager.clientSettingsManager().exportSettingsProto(cliManager.cliInstanceId()))
      .build());

    if (start) {
      cliManager.clientCommandManager().execute("start-attack");
    } else {
      log.info(
        "SoulFire is ready to go! Type 'start-attack' to start the attack! (Use --start to start automatically)");
    }

    var commandManager = cliManager.clientCommandManager();
    new GenericTerminalConsole(
      cliManager.shutdownManager(),
      commandManager::execute,
      commandManager::complete,
      SFPathConstants.CLIENT_DATA_DIRECTORY
    ).start();

    cliManager.shutdownManager().awaitShutdown();

    return 0;
  }
}
