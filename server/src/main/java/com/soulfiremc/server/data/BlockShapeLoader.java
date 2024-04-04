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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class BlockShapeLoader {
  public static final Map<ResourceKey, List<BlockShapeGroup>> BLOCK_SHAPES =
    new Object2ObjectOpenHashMap<>();

  static {
    try (var inputStream =
           BlockShapeGroup.class.getClassLoader().getResourceAsStream("minecraft/blockstates.txt")) {
      if (inputStream == null) {
        throw new IllegalStateException("blockstates.txt not found!");
      }

      new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        .lines()
        .forEach(
          line -> {
            var parts = line.split("\\|");
            var key = ResourceKey.fromString(parts[0]);

            var blockShapeTypes = new ObjectArrayList<BlockShapeGroup>();
            if (parts.length > 1) {
              var part = parts[1];

              var subParts = part.split(",");
              for (var subPart : subParts) {
                var id = Integer.parseInt(subPart);
                var blockShapeType = BlockShapeGroup.getById(id);
                blockShapeTypes.add(blockShapeType);
              }
            }

            BLOCK_SHAPES.put(key, blockShapeTypes);
          });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
