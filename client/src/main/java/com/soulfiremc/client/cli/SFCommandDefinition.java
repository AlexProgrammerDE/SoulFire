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

import com.soulfiremc.brigadier.ClientConsoleCommandSource;
import com.soulfiremc.brigadier.GenericTerminalConsole;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.client.settings.ProxyParser;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.proxy.ProxyType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@RequiredArgsConstructor
@Command(
  name = "soulfire",
  mixinStandardHelpOptions = true,
  version = "SoulFire v" + BuildData.VERSION,
  showDefaultValues = true,
  description = BuildData.DESCRIPTION,
  sortOptions = false)
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
    names = {"--profile-file"},
    description = "File to load a profile from")
  private Path profileFile;

  @Option(
    names = {"--generate-flags"},
    description = "Create a list of flags",
    hidden = true)
  private boolean generateFlags;

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
                .collect(Collectors.joining(", "));
            var defaultValue =
              option.defaultValueString() == null
                ? ""
                : "`%s`".formatted(option.defaultValueString());
            var description =
              option.description() == null ? "" : String.join(", ", option.description());
            System.out.printf("| %s | %s | %s |%n", name, defaultValue, description);
          });
      cliManager.shutdown();
      return 0;
    }

    // Delayed to here, so help and version do not get cut off
    GenericTerminalConsole.setupStreams();

    if (accountFile != null && authType != null) {
      try {
        cliManager
          .clientSettingsManager()
          .accountRegistry()
          .loadFromString(Files.readString(accountFile), authType, null);
      } catch (IOException e) {
        log.error("Failed to load accounts!", e);
        return 1;
      }
    }

    if (proxyFile != null) {
      try {
        cliManager
          .clientSettingsManager()
          .proxyRegistry()
          .loadFromString(
            Files.readString(proxyFile),
            proxyType == null ? ProxyParser.uriParser() : ProxyParser.typeParser(proxyType));
      } catch (IOException e) {
        log.error("Failed to load proxies!", e);
        return 1;
      }
    }

    if (profileFile != null) {
      try {
        cliManager.clientSettingsManager().loadProfile(profileFile);
      } catch (IOException e) {
        log.error("Failed to load profile!", e);
        return 1;
      }
    }

    if (start) {
      cliManager.clientCommandManager().execute("start-attack", new ClientConsoleCommandSource());
    } else {
      log.info(
        "SoulFire is ready to go! Type 'start-attack' to start the attack! (Use --start to start automatically)");
    }

    new GenericTerminalConsole<>(cliManager.shutdownManager(), ClientConsoleCommandSource.INSTANCE,
      cliManager.clientCommandManager(), cliManager.commandHistoryManager())
      .start();

    cliManager.shutdownManager().awaitShutdown();

    return 0;
  }
}
