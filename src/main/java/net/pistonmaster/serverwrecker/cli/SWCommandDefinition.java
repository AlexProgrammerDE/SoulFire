/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.cli;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.command.SWTerminalConsole;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.proxy.ProxyType;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Command(name = "serverwrecker", mixinStandardHelpOptions = true,
        version = "ServerWrecker v" + BuildData.VERSION, showDefaultValues = true,
        description = BuildData.DESCRIPTION, sortOptions = false)
public class SWCommandDefinition implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SWCommandDefinition.class);
    private final ServerWreckerServer serverWreckerServer;
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
            serverWreckerServer.shutdown();
            return 0;
        }

        // Delayed to here, so help and version do not get cut off
        var gRPCHost = serverWreckerServer.getRpcServer().getHost();
        var gRPCPort = serverWreckerServer.getRpcServer().getPort();
        var rpcClient = new RPCClient(gRPCHost, gRPCPort, serverWreckerServer.generateAdminJWT());
        SWTerminalConsole.setupTerminalConsole(serverWreckerServer.getThreadPool(), serverWreckerServer.getShutdownManager(), rpcClient);

        if (accountFile != null && authType != null) {
            try {
                serverWreckerServer.getAccountRegistry().loadFromString(Files.readString(accountFile), authType);
            } catch (IOException e) {
                LOGGER.error("Failed to load accounts!", e);
                return 1;
            }
        }

        if (proxyFile != null && proxyType != null) {
            try {
                serverWreckerServer.getProxyRegistry().loadFromString(Files.readString(proxyFile), proxyType);
            } catch (IOException e) {
                LOGGER.error("Failed to load proxies!", e);
                return 1;
            }
        }

        if (profileFile != null) {
            try {
                serverWreckerServer.getSettingsManager().loadProfile(profileFile);
            } catch (IOException e) {
                LOGGER.error("Failed to load profile!", e);
                return 1;
            }
        }

        if (start) {
            serverWreckerServer.startAttack();
        } else {
            LOGGER.info("ServerWrecker is ready to go! Type 'start-attack' to start the attack!");
        }

        return 0;
    }
}
