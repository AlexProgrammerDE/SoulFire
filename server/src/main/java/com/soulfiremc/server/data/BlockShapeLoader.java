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

import com.soulfiremc.server.util.SFHelpers;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockShapeLoader {
  public static final Map<Key, List<BlockShapeGroup>> BLOCK_COLLISION_SHAPES =
    new Object2ObjectOpenHashMap<>();
  public static final Map<Key, List<BlockShapeGroup>> BLOCK_SUPPORT_SHAPES =
    new Object2ObjectOpenHashMap<>();

  static {
    SFHelpers.getResourceAsString("minecraft/block-states.txt")
      .lines()
      .forEach(
        line -> {
          var parts = line.split("\\|");
          @Subst("empty") var keyString = parts[0];
          var key = Key.key(keyString);

          var blockCollisionShapeTypes = new ArrayList<BlockShapeGroup>();
          var blockSupportShapeTypes = new ArrayList<BlockShapeGroup>();
          if (parts.length > 1) {
            {
              var subParts = parts[1].split(",");
              for (var subPart : subParts) {
                var id = Integer.parseInt(subPart);
                var blockShapeType = BlockShapeGroup.getById(id);
                blockCollisionShapeTypes.add(blockShapeType);
              }
            }

            {
              var subParts = parts[2].split(",");
              for (var subPart : subParts) {
                var id = Integer.parseInt(subPart);
                var blockShapeType = BlockShapeGroup.getById(id);
                blockSupportShapeTypes.add(blockShapeType);
              }
            }
          }

          BLOCK_COLLISION_SHAPES.put(key, blockCollisionShapeTypes);
          BLOCK_SUPPORT_SHAPES.put(key, blockSupportShapeTypes);
        });
  }
}
