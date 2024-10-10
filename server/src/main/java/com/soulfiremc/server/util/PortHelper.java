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
package com.soulfiremc.server.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
public class PortHelper {
  public static final int SF_DEFAULT_PORT = 38765;

  public static int getAvailablePort(int startPort) {
    while (true) {
      try {
        var serverSocket = new ServerSocket(startPort);
        serverSocket.close();
        break; // Port is available, exit the loop
      } catch (IOException e) {
        log.info("Port {} is already in use, trying next port...", startPort);
        startPort++; // Increment the port number and try again
      }
    }

    return startPort;
  }

  public static int getRandomAvailablePort() {
    try {
      var serverSocket = new ServerSocket(0);
      var port = serverSocket.getLocalPort();
      serverSocket.close();
      return port;
    } catch (IOException e) {
      throw new RuntimeException("Could not find an available port", e);
    }
  }
}
