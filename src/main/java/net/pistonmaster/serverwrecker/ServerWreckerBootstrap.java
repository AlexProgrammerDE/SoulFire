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

import io.netty.util.ResourceLeakDetector;
import net.pistonmaster.serverwrecker.addons.*;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.cli.SWCommandDefinition;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.gui.GUIManager;
import net.pistonmaster.serverwrecker.gui.theme.ThemeUtil;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ServerWreckerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWreckerBootstrap.class);

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        // If Velocity's natives are being extracted to a different temporary directory, make sure the
        // Netty natives are extracted there as well
        if (System.getProperty("velocity.natives-tmpdir") != null) {
            System.setProperty("io.netty.native.workdir", System.getProperty("velocity.natives-tmpdir"));
        }

        // Disable the resource leak detector by default as it reduces performance. Allow the user to
        // override this if desired.
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
    }

    public static void main(String[] args) {
        if (System.console() != null) {
            AnsiConsole.systemInstall();
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                LOGGER.error("Exception in thread {}", thread.getName(), throwable));

        int port = getAvailablePort();
        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            loadInternalAddons();
            runHeadless(port, args);
        } else {
            ThemeUtil.initFlatLaf();
            ThemeUtil.setLookAndFeel();

            loadInternalAddons();
            runGUI(port);
        }
    }

    private static void loadInternalAddons() {
        List<InternalAddon> addons = List.of(
                new BotTicker(), new ClientBrand(), new ClientSettings(),
                new AutoReconnect(), new AutoRegister(), new AutoRespawn(),
                new AutoTotem(), new AutoJump(), new AutoArmor(), new AutoEat(),
                new ChatMessageLogger(), new ServerListBypass());

        addons.forEach(ServerWreckerAPI::registerAddon);
    }

    private static void runHeadless(int port, String[] args) {
        ServerWrecker serverWrecker = new ServerWrecker(OperationMode.CLI, "localhost", port);
        SWCommandDefinition serverWreckerCommand = new SWCommandDefinition(serverWrecker);
        CommandLine commandLine = new CommandLine(serverWreckerCommand);
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

    private static void runGUI(int port) {
        ServerWrecker serverWrecker = new ServerWrecker(OperationMode.GUI, "localhost", port);
        serverWrecker.initConsole();

        GUIManager guiManager = new GUIManager(serverWrecker, serverWrecker.getInjector().getSingleton(RPCClient.class));
        guiManager.initGUI();
    }

    private static int getAvailablePort() {
        int initialPort = 38765;

        while (true) {
            try {
                ServerSocket serverSocket = new ServerSocket(initialPort);
                serverSocket.close();
                break; // Port is available, exit the loop
            } catch (IOException e) {
                System.out.println("Port " + initialPort + " is already in use, trying next port...");
                initialPort++; // Increment the port number and try again
            }
        }

        return initialPort;
    }
}
