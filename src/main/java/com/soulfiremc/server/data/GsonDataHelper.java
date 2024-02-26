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
package com.soulfiremc.server.data;

import com.google.gson.JsonArray;
import java.util.HashMap;
import java.util.Map;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.ResourceHelper;

public class GsonDataHelper {
  private static final Map<String, JsonArray> LOADED_DATA = new HashMap<>();

  public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz) {
    var array =
        LOADED_DATA.computeIfAbsent(
            dataFile,
            file -> {
              var data = new JsonArray();
              try {
                data = GsonInstance.GSON.fromJson(ResourceHelper.getResource(file), JsonArray.class);
              } catch (Exception e) {
                e.printStackTrace();
              }
              return data;
            });
    for (var element : array) {
      if (element.getAsJsonObject().get("name").getAsString().equals(dataKey)) {
        return GsonInstance.GSON.fromJson(element, clazz);
      }
    }

    throw new RuntimeException("Failed to find data key " + dataKey + " in file " + dataFile);
  }
}
