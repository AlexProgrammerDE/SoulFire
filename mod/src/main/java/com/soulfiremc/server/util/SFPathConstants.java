/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util;

import java.nio.file.Path;

public final class SFPathConstants {
  public static final Path BASE_DIR = Path.of(System.getProperty("sf.baseDir"));

  private SFPathConstants() {}

  public static Path getConfigDirectory() {
    return SFPathConstants.BASE_DIR.resolve("config");
  }

  public static Path getMapsDirectory(Path baseDir) {
    return baseDir.resolve("maps");
  }

  public static Path getRendersDirectory(Path baseDir) {
    return baseDir.resolve("renders");
  }
}
