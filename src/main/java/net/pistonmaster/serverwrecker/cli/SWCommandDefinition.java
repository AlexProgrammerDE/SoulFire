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
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.builddata.BuildData;
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
    private final ServerWrecker serverWrecker;
    @Setter
    private CommandLine commandLine;

    @Option(names = {"--host", "--target"}, description = "Target url to connect to")
    private String host = BotSettings.DEFAULT_HOST;

    @Option(names = {"--port"}, description = "Target port to connect to")
    private int port = BotSettings.DEFAULT_PORT;

    @Option(names = {"-a", "--amount"}, description = "Amount of bots to connect to the server")
    private int amount = BotSettings.DEFAULT_AMOUNT;

    @Option(names = {"--min-join-delay"}, description = "The minimum delay between bot connections, in milliseconds")
    private int minJoinDelay = BotSettings.DEFAULT_MIN_JOIN_DELAY_MS;

    @Option(names = {"--max-join-delay"}, description = "The maximum delay between bot connections, in milliseconds")
    private int maxJoinDelay = BotSettings.DEFAULT_MAX_JOIN_DELAY_MS;

    @Option(names = {"-mc", "--mc-version"}, description = "Minecraft version of the server to connect to")
    private String version = BotSettings.DEFAULT_PROTOCOL_VERSION.getName();

    @Option(names = {"--read-timeout"}, description = "Bot read timeout")
    private int readTimeout = BotSettings.DEFAULT_READ_TIMEOUT;

    @Option(names = {"--write-timeout"}, description = "Bot write timeout")
    private int writeTimout = BotSettings.DEFAULT_WRITE_TIMEOUT;

    @Option(names = {"--connect-timeout"}, description = "Bot connect timeout")
    private int connectTimeout = BotSettings.DEFAULT_CONNECT_TIMEOUT;

    @Option(names = {"--try-srv"}, description = "Try to connect to the target using SRV records")
    private boolean trySrv = BotSettings.DEFAULT_TRY_SRV;

    @Option(names = {"--concurrent-connects"}, description = "Amount of bots that can try to connect at the same time")
    private int concurrentConnects = BotSettings.DEFAULT_CONCURRENT_CONNECTS;

    @Option(names = {"--via-debug"}, description = "Set Via* to debug mode")
    private boolean viaDebug = DevSettings.DEFAULT_VIA_DEBUG;

    @Option(names = {"--netty-debug"}, description = "Set Netty to debug mode")
    private boolean nettyDebug = DevSettings.DEFAULT_NETTY_DEBUG;

    @Option(names = {"--grpc-debug"}, description = "Set gRPC to debug mode")
    private boolean gRPCDebug = DevSettings.DEFAULT_GRPC_DEBUG;

    @Option(names = {"--core-debug"}, description = "Set core loggers to debug mode")
    private boolean coreDebug = DevSettings.DEFAULT_CORE_DEBUG;

    @Option(names = {"--bots-per-proxy"}, description = "Amount of bots that can be on a single proxy")
    private int botsPerProxy = ProxySettings.DEFAULT_BOTS_PER_PROXY;

    @Option(names = {"--name-format"}, description = "Format for bot names. allows integer placeholder '%%d'")
    private String nameFormat = AccountSettings.DEFAULT_NAME_FORMAT;

    @Option(names = {"--shuffle-accounts"}, description = "Shuffle accounts before connecting")
    private boolean shuffleAccounts = AccountSettings.DEFAULT_SHUFFLE_ACCOUNTS;

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
            serverWrecker.shutdown(true);
            return 0;
        }

        // Delayed to here, so help and version do not get cut off
        serverWrecker.initConsole();

        serverWrecker.getSettingsManager().registerProvider(BotSettings.class,
                () -> new BotSettings(
                        host,
                        port,
                        amount,
                        minJoinDelay,
                        maxJoinDelay,
                        ProtocolVersion.getClosest(version),
                        readTimeout,
                        writeTimout,
                        connectTimeout,
                        trySrv,
                        concurrentConnects
                ));

        serverWrecker.getSettingsManager().registerProvider(DevSettings.class,
                () -> new DevSettings(
                        viaDebug,
                        nettyDebug,
                        gRPCDebug,
                        coreDebug
                ));

        serverWrecker.getSettingsManager().registerProvider(AccountSettings.class,
                () -> new AccountSettings(
                        nameFormat,
                        shuffleAccounts
                ));

        serverWrecker.getSettingsManager().registerProvider(ProxySettings.class,
                () -> new ProxySettings(
                        botsPerProxy
                ));

        if (accountFile != null && authType != null) {
            try {
                serverWrecker.getAccountRegistry().loadFromString(Files.readString(accountFile), authType);
            } catch (IOException e) {
                LOGGER.error("Failed to load accounts!", e);
                return 1;
            }
        }

        if (proxyFile != null && proxyType != null) {
            try {
                serverWrecker.getProxyRegistry().loadFromString(Files.readString(proxyFile), proxyType);
            } catch (IOException e) {
                LOGGER.error("Failed to load proxies!", e);
                return 1;
            }
        }

        if (profileFile != null) {
            try {
                serverWrecker.getSettingsManager().loadProfile(profileFile);
            } catch (IOException e) {
                LOGGER.error("Failed to load profile!", e);
                return 1;
            }
        }

        serverWrecker.startAttack();
        return 0;
    }
}
