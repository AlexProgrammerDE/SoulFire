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

import net.pistonmaster.serverwrecker.addons.*;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.cli.SWCommandDefinition;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.gui.GUIManager;
import net.pistonmaster.serverwrecker.gui.theme.ThemeUtil;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ServerWreckerLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWreckerLoader.class);

    private ServerWreckerLoader() {
    }

    public static void injectJvm() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                LOGGER.error("Exception in thread {}", thread.getName(), throwable));

        if (System.console() != null) {
            AnsiConsole.systemInstall();
        }

        ServerWrecker.setupLogging(DevSettings.DEFAULT);
    }

    public static void injectTheme() {
        ThemeUtil.initFlatLaf();
        ThemeUtil.setLookAndFeel();
    }

    public static void loadInternalAddons() {
        var addons = List.of(
                new BotTicker(), new ClientBrand(), new ClientSettings(),
                new AutoReconnect(), new AutoRegister(), new AutoRespawn(),
                new AutoTotem(), new AutoJump(), new AutoArmor(), new AutoEat(),
                new ChatMessageLogger(), new ServerListBypass());

        addons.forEach(ServerWreckerAPI::registerAddon);
    }

    public static void runHeadless(int port, String[] args) {
        var serverWrecker = new ServerWrecker(OperationMode.CLI, "localhost", port);
        var serverWreckerCommand = new SWCommandDefinition(serverWrecker);
        var commandLine = new CommandLine(serverWreckerCommand);
        serverWreckerCommand.setCommandLine(commandLine);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setUsageHelpAutoWidth(true);
        commandLine.setUsageHelpLongOptionsMaxWidth(30);
        commandLine.setExecutionExceptionHandler((ex, cmdLine, parseResult) -> {
            LOGGER.error("Exception while executing command", ex);
            return 1;
        });

        ServerWreckerAPI.postEvent(new CommandManagerInitEvent(commandLine));
        commandLine.execute(args);
    }

    public static void runGUI(int port) {
        var serverWrecker = new ServerWrecker(OperationMode.GUI, "localhost", port);
        serverWrecker.initConsole();

        var guiManager = new GUIManager(serverWrecker, serverWrecker.getInjector().getSingleton(RPCClient.class));
        guiManager.initGUI();
    }

    public static int getAvailablePort() {
        var initialPort = 38765;

        while (true) {
            try {
                var serverSocket = new ServerSocket(initialPort);
                serverSocket.close();
                break; // Port is available, exit the loop
            } catch (IOException e) {
                LOGGER.info("Port {} is already in use, trying next port...", initialPort);
                initialPort++; // Increment the port number and try again
            }
        }

        return initialPort;
    }
}
