/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.Options;
import net.pistonmaster.serverwrecker.common.ProxyType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@SuppressWarnings("FieldMayBeFinal")
@RequiredArgsConstructor
@Command(name = "serverwrecker", mixinStandardHelpOptions = true, version = "ServerWrecker v" + ServerWrecker.VERSION, description = "Stress test a minecraft server using bots")
public class CommandDefinition implements Callable<Integer> {
    private final File dataFolder;

    @Option(names = {"-h", "--host"}, description = "The hostname to connect to. Defaults to 127.0.0.1")
    private String host = "127.0.0.1";

    @Option(names = {"-p", "--port"}, description = "The port to connect to. Defaults to 25565")
    private int port = 25565;

    @Option(names = {"-a", "--amount"}, description = "The amount of bots to connect to the server. Defaults to 20")
    private int amount = 20;

    @Option(names = {"-d", "--delay"}, description = "The delay between bot spawns, in milliseconds. Defaults to 1000")
    private int joinDelay = 1000;

    @Option(names = {"-n", "--name"}, description = "The format for bot names. Requires exactly one integer placeholder '%%d'. Defaults to 'Bot-%%d'")
    private String nameFormat = "Bot-%d";

    @Option(names = {"-v", "--mcversion"}, description = "The Minecraft version of the server to connect to. Defaults to latest")
    private GameVersion version = GameVersion.getNewest();

    @Option(names = {"-r", "--register"}, description = "Makes Bots run the /register and /login command after joining with username and password being " + ServerWrecker.PROJECT_NAME)
    private boolean autoRegister;

    @Option(names = {"--help"}, usageHelp = true, description = "Shows this help message.")
    private boolean help;

    @Option(names = {"--debug"}, description = "Logs additional information useful for debugging")
    private boolean debug;

    @Option(names = {"--proxytype"}, description = "The proxies type. Defaults to socks5")
    private ProxyType proxy = ProxyType.SOCKS5;

    @Option(names = {"--accountsperproxy"}, description = "How many accounts can be on a single proxy. Defaults to -1")
    private int accountsPerProxy = -1;

    @Option(names = {"--readtimeout"}, description = "The client read timeout.")
    private int readTimeout = 30;

    @Option(names = {"--writetimout"}, description = "The client write timeout.")
    private int writeTimout = 0;

    @Option(names = {"--connecttimeout"}, description = "The client connect timeout.")
    private int connectTimeout = 30;

    @Option(names = {"--compressionthreshold"}, description = "The client compression threshold.")
    private int compressionThreshold = -1;

    @Option(names = {"--registercommand"}, description = "What command should be executed to register?")
    private String registerCommand = "/register %password% %password%";

    @Option(names = {"--logincommand"}, description = "What command should be executed to log in?")
    private String loginCommand = "/login %password%";

    @Option(names = {"--captchacommand"}, description = "What command should be executed to confirm the captcha?")
    private String captchaCommand = "/captcha %captcha%";

    @Option(names = {"--passwordformet"}, description = "What the password for registering should be.")
    private String passwordFormat = "ServerWrecker";

    @Override
    public Integer call() {
        Main.initPlugins(dataFolder);
        ServerWrecker.getInstance().start(new Options(
                host,
                port,
                amount,
                joinDelay,
                nameFormat,
                version,
                autoRegister,
                debug,
                proxy,
                accountsPerProxy,
                readTimeout,
                writeTimout,
                connectTimeout,
                compressionThreshold,
                registerCommand,
                loginCommand,
                captchaCommand,
                passwordFormat
        ));
        return 0;
    }
}