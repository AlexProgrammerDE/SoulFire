/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker;

import net.pistonmaster.serverwrecker.client.cli.CLIManager;
import net.pistonmaster.serverwrecker.client.grpc.RPCClient;
import net.pistonmaster.serverwrecker.client.gui.GUIManager;
import net.pistonmaster.serverwrecker.server.ServerWreckerServer;

public class ServerWreckerLoader {
    private ServerWreckerLoader() {
    }

    public static void runHeadless(String host, int port, String[] args) {
        var serverWrecker = new ServerWreckerServer(host, port);

        var rpcClient = new RPCClient(host, port, serverWrecker.generateLocalCliJWT());
        var cliManager = new CLIManager(rpcClient);
        cliManager.initCLI(args);
    }

    public static void runGUI(String host, int port) {
        var serverWrecker = new ServerWreckerServer(host, port);

        var rpcClient = new RPCClient(host, port, serverWrecker.generateAdminJWT());
        var guiManager = new GUIManager(rpcClient);
        guiManager.initGUI();
    }
}
