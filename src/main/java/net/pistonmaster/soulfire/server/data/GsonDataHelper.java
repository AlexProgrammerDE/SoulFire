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
package net.pistonmaster.soulfire.server.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.pistonmaster.soulfire.util.ResourceHelper;

import java.util.HashMap;
import java.util.Map;

public class GsonDataHelper {
    private static final Map<String, JsonArray> LOADED_DATA = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz) {
        var array = LOADED_DATA.computeIfAbsent(dataFile, file -> {
            var data = new JsonArray();
            try {
                data = GSON.fromJson(ResourceHelper.getResource(file), JsonArray.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        });
        for (var element : array) {
            if (element.getAsJsonObject().get("name").getAsString().equals(dataKey)) {
                return GSON.fromJson(element, clazz);
            }
        }

        throw new RuntimeException("Failed to find data key " + dataKey + " in file " + dataFile);
    }
}
