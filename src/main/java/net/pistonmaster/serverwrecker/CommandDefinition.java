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
package net.pistonmaster.serverwrecker;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@SuppressWarnings("FieldMayBeFinal")
@RequiredArgsConstructor
@Command(name = "serverwrecker", mixinStandardHelpOptions = true,
        version = "ServerWrecker v" + BuildData.VERSION, showDefaultValues = true,
        description = "Stress test a minecraft server using bots", sortOptions = false)
public class CommandDefinition implements Callable<Integer> {
    private final Path dataFolder;

    @Option(names = {"--host", "--target"}, description = "target url to connect to")
    private String host = "127.0.0.1";

    @Option(names = {"--port"}, description = "target port to connect to")
    private int port = 25565;

    @Option(names = {"-a", "--amount"}, description = "amount of bots to connect to the server")
    private int amount = 20;

    @Option(names = {"--join-delay"}, description = "the delay between bot spawns, in milliseconds")
    private int joinDelay = 1000;

    @Option(names = {"--name-format"}, description = "format for bot names. allows integer placeholder '%%d'")
    private String nameFormat = "Bot-%d";

    @Option(names = {"-mc", "--mc-version"}, description = "Minecraft version of the server to connect to")
    private String version = SWConstants.LATEST_SHOWN_VERSION.getName();

    @Option(names = {"--debug"}, description = "log additional information useful for debugging the software")
    private boolean debug;

    @Option(names = {"--proxy-type"}, description = "type of proxies used")
    private ProxyType proxy = ProxyType.SOCKS5;

    @Option(names = {"--accounts-per-proxy"}, description = "amount of accounts that can be on a single proxy")
    private int accountsPerProxy = -1;

    @Option(names = {"--read-timeout"}, description = "bot read timeout")
    private int readTimeout = 30;

    @Option(names = {"--write-timeout"}, description = "bot write timeout")
    private int writeTimout = 0;

    @Option(names = {"--connect-timeout"}, description = "bot connect timeout")
    private int connectTimeout = 30;

    @Option(names = {"--auto-register"}, description = "make bots run the /register and /login command after joining")
    private boolean autoRegister;

    @Option(names = {"--register-command"}, description = "command to be executed to register")
    private String registerCommand = "/register %password% %password%";

    @Option(names = {"--login-command"}, description = "command to be executed to log in")
    private String loginCommand = "/login %password%";

    @Option(names = {"--captcha-command"}, description = "command to be executed to confirm a captcha")
    private String captchaCommand = "/captcha %captcha%";

    @Option(names = {"--password-format"}, description = "the password for registering")
    private String passwordFormat = "ServerWrecker";

    @Option(names = {"--auto-reconnect"}, description = "reconnect bots after being disconnected")
    private boolean autoReconnect = true;

    @Option(names = {"--auto-respawn"}, description = "respawn bots after death")
    private boolean autoRespawn = true;

    @Option(names = {"--disable-wait-established"}, description = "make the program halt and wait till a bot was successfully connected before connecting the next bot")
    private boolean disableWaitEstablished;

    @Option(names = {"--auth-service"}, description = "the auth service to use")
    private AuthType authType = AuthType.OFFLINE;

    @Override
    public Integer call() {
        ServerWrecker serverWrecker = new ServerWrecker(dataFolder);

        serverWrecker.getSettingsManager().registerProvider(BotSettings.class,
                () -> new BotSettings(
                        host,
                        port,
                        amount,
                        joinDelay,
                        !disableWaitEstablished,
                        nameFormat,
                        ProtocolVersion.getClosest(version),
                        autoRegister,
                        proxy,
                        accountsPerProxy,
                        readTimeout,
                        writeTimout,
                        connectTimeout,
                        registerCommand,
                        loginCommand,
                        captchaCommand,
                        passwordFormat,
                        autoReconnect,
                        autoRespawn,
                        authType
                ));

        serverWrecker.getSettingsManager().registerProvider(DevSettings.class,
                () -> new DevSettings(
                        debug
                ));

        serverWrecker.start();
        return 0;
    }
}
