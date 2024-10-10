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

import java.nio.file.Path;

public class SFPathConstants {
  public static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"));
  public static final Path CLIENT_DATA_DIRECTORY = Path.of(System.getProperty("user.home")).resolve(".soulfire");
  public static final Path INTEGRATED_SERVER_DIRECTORY = CLIENT_DATA_DIRECTORY.resolve("integrated-server");

  private SFPathConstants() {}

  public static Path getPluginsDirectory(Path baseFolder) {
    return baseFolder.resolve("plugins");
  }

  public static Path getConfigDirectory(Path baseFolder) {
    return baseFolder.resolve("config");
  }

  public static Path getSecretKeyFile(Path baseFolder) {
    return baseFolder.resolve("secret-key.bin");
  }

  public static Path getMapsDirectory(Path baseFolder) {
    return baseFolder.resolve("maps");
  }

  public static Path getStateDirectory(Path baseFolder) {
    return baseFolder.resolve("state");
  }

  public static Path getLibrariesDirectory(Path baseFolder) {
    return baseFolder.resolve("libraries");
  }
}
