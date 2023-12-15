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

import net.pistonmaster.serverwrecker.cli.CLIManager;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.gui.GUIClientProps;
import net.pistonmaster.serverwrecker.gui.GUIManager;
import net.pistonmaster.serverwrecker.gui.ThemeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerWreckerLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWreckerLoader.class);

    private ServerWreckerLoader() {
    }

    public static void injectTheme() {
        ThemeUtil.initFlatLaf();
        ThemeUtil.setLookAndFeel();
    }

    public static void loadGUIProperties() {
        GUIClientProps.loadSettings();
    }

    public static void runHeadless(int port, String[] args) {
        var host = "localhost";
        var serverWrecker = new ServerWreckerServer(OperationMode.CLI, host, port);

        var rpcClient = new RPCClient(host, port, serverWrecker.generateAdminJWT());
        var cliManager = new CLIManager(rpcClient);
        cliManager.initCLI(args);
    }

    public static void runGUI(int port) {
        var host = "localhost";
        var serverWrecker = new ServerWreckerServer(OperationMode.GUI, host, port);

        var rpcClient = new RPCClient(host, port, serverWrecker.generateAdminJWT());
        var guiManager = new GUIManager(rpcClient);
        guiManager.initGUI();
    }
}
