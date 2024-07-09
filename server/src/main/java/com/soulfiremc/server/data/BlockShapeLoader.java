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

import com.soulfiremc.util.ResourceHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;

public class BlockShapeLoader {
  public static final Map<Key, List<BlockShapeGroup>> BLOCK_SHAPES =
    new Object2ObjectOpenHashMap<>();

  static {
    ResourceHelper.getResourceAsString("minecraft/blockstates.txt")
      .lines()
      .forEach(
        line -> {
          var parts = line.split("\\|");
          @Subst("empty") var keyString = parts[0];
          var key = Key.key(keyString);

          var blockShapeTypes = new ArrayList<BlockShapeGroup>();
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
  }
}
