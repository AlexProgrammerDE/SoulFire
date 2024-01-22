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
package net.pistonmaster.soulfire;

import net.pistonmaster.soulfire.client.cli.CLIManager;
import net.pistonmaster.soulfire.client.grpc.RPCClient;
import net.pistonmaster.soulfire.client.gui.GUIManager;
import net.pistonmaster.soulfire.server.SoulFireServer;

public class SoulFireLoader {
    private SoulFireLoader() {
    }

    public static void runHeadless(String host, int port, String[] args) {
        var soulFire = new SoulFireServer(host, port);

        var rpcClient = new RPCClient(host, port, soulFire.generateLocalCliJWT());
        var cliManager = new CLIManager(rpcClient);
        cliManager.initCLI(args);
    }

    public static void runGUI(String host, int port) {
        var soulFire = new SoulFireServer(host, port);

        var rpcClient = new RPCClient(host, port, soulFire.generateAdminJWT());
        var guiManager = new GUIManager(rpcClient);
        guiManager.initGUI();
    }
}
