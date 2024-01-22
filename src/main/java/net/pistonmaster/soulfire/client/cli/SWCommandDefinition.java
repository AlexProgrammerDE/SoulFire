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
package net.pistonmaster.soulfire.client.cli;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.builddata.BuildData;
import net.pistonmaster.soulfire.client.SWTerminalConsole;
import net.pistonmaster.soulfire.proxy.ProxyType;
import net.pistonmaster.soulfire.server.viaversion.SWVersionConstants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Command(name = "soulfire", mixinStandardHelpOptions = true,
        version = "SoulFire v" + BuildData.VERSION, showDefaultValues = true,
        description = BuildData.DESCRIPTION, sortOptions = false)
public class SWCommandDefinition implements Callable<Integer> {
    private final CLIManager cliManager;
    @Setter
    private CommandLine commandLine;

    @Option(names = {"-s", "--start"}, description = "Whether to start the attack automatically")
    private boolean start;

    @Option(names = {"--account-file"}, description = "File to load accounts from")
    private Path accountFile;

    @Option(names = {"--account-type"}, description = "Type of accounts in the account file")
    private AuthType authType;

    @Option(names = {"--proxy-file"}, description = "File to load proxies from")
    private Path proxyFile;

    @Option(names = {"--proxy-type"}, description = "Type of proxies in the proxy file")
    private ProxyType proxyType;

    @Option(names = {"--profile-file"}, description = "File to load a profile from")
    private Path profileFile;

    @Option(names = {"--generate-flags"}, description = "Create a list of flags", hidden = true)
    private boolean generateFlags;

    @Option(names = {"--generate-versions"}, description = "Create a list of supported versions", hidden = true)
    private boolean generateVersions;

    @Override
    public Integer call() {
        if (generateFlags) {
            commandLine.getCommandSpec().options().forEach(option -> {
                if (option.hidden()) {
                    return;
                }

                var name = Arrays.stream(option.names()).map(s -> String.format("`%s`", s)).collect(Collectors.joining(", "));
                var defaultValue = option.defaultValueString() == null ? "" : String.format("`%s`", option.defaultValueString());
                var description = option.description() == null ? "" : String.join(", ", option.description());
                System.out.printf("| %s | %s | %s |%n", name, defaultValue, description);
            });
            cliManager.shutdown();
            return 0;
        } else if (generateVersions) {
            var yesEmoji = "✅";
            var noEmoji = "❌";

            SWVersionConstants.getVersionsSorted().forEach(version -> {
                var nativeVersion = SWVersionConstants.CURRENT_PROTOCOL_VERSION == version ? yesEmoji : noEmoji;
                var bedrockVersion = SWVersionConstants.isBedrock(version) ? yesEmoji : noEmoji;
                var javaVersion = !SWVersionConstants.isBedrock(version) ? yesEmoji : noEmoji;
                var snapshotVersion = SWVersionConstants.isAprilFools(version) ? yesEmoji : noEmoji;
                var legacyVersion = SWVersionConstants.isLegacy(version) ? yesEmoji : noEmoji;

                System.out.printf("| %s | %s | %s | %s | %s | %s |%n", version.getName(), nativeVersion, javaVersion, snapshotVersion, legacyVersion, bedrockVersion);
            });
            cliManager.shutdown();
            return 0;
        }

        // Delayed to here, so help and version do not get cut off
        SWTerminalConsole.setupTerminalConsole(
                cliManager.threadPool(),
                cliManager.shutdownManager(),
                cliManager.clientCommandManager()
        );

        if (accountFile != null && authType != null) {
            try {
                cliManager.settingsManager().accountRegistry().loadFromString(Files.readString(accountFile), authType);
            } catch (IOException e) {
                log.error("Failed to load accounts!", e);
                return 1;
            }
        }

        if (proxyFile != null && proxyType != null) {
            try {
                cliManager.settingsManager().proxyRegistry().loadFromString(Files.readString(proxyFile), proxyType);
            } catch (IOException e) {
                log.error("Failed to load proxies!", e);
                return 1;
            }
        }

        if (profileFile != null) {
            try {
                cliManager.settingsManager().loadProfile(profileFile);
            } catch (IOException e) {
                log.error("Failed to load profile!", e);
                return 1;
            }
        }

        if (start) {
            cliManager.clientCommandManager().execute("start-attack");
        } else {
            log.info("SoulFire is ready to go! Type 'start-attack' to start the attack! (Use --start to start automatically)");
        }

        return 0;
    }
}
