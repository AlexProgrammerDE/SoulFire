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
package com.soulfiremc.util;

import java.nio.file.Path;
import net.harawata.appdirs.AppDirsFactory;

public class SFPathConstants {
  public static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"));
  public static final Path CLIENT_DATA_DIRECTORY = getApplicationDataDirectory();
  public static final Path PROFILES_DIRECTORY = CLIENT_DATA_DIRECTORY.resolve("profiles");
  public static final Path INTEGRATED_SERVER_DIRECTORY = CLIENT_DATA_DIRECTORY.resolve("integrated-server");

  private SFPathConstants() {}

  private static Path getApplicationDataDirectory() {
    return Path.of(AppDirsFactory.getInstance().getUserDataDir("SoulFire", null, null));
  }

  public static Path getPluginsDirectory(Path baseFolder) {
    return baseFolder.resolve("plugins");
  }

  public static Path getConfigDirectory(Path baseFolder) {
    return baseFolder.resolve("config");
  }

  public static Path getMapsDirectory(Path baseFolder) {
    return baseFolder.resolve("maps");
  }
}
