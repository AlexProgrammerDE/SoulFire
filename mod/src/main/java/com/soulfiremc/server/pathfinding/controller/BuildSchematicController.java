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
package com.soulfiremc.server.pathfinding.controller;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public final class BuildSchematicController {
  private final Map<BlockPos, Block> absoluteBlocks;

  public BuildSchematicController(Map<BlockPos, Block> relativeBlocks, BlockPos base) {
    this(new Object2ObjectOpenHashMap<>());

    for (var entry : relativeBlocks.entrySet()) {
      this.absoluteBlocks.put(entry.getKey().offset(base), entry.getValue());
    }
  }
}
