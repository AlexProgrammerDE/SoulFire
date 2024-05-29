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
package com.soulfiremc.generator.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ResourceHelper {
  private ResourceHelper() {}

  public static String getResourceAsString(String path) {
    try {
      var inputStream = Objects.requireNonNull(ResourceHelper.class.getResourceAsStream(path));
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate language file", e);
    }
  }
}
