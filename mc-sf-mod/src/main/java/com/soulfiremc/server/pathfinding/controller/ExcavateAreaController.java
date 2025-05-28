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
package com.soulfiremc.server.pathfinding.controller;

import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.BreakBlockPosGoal;
import com.soulfiremc.server.pathfinding.goals.CompositeGoal;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class ExcavateAreaController {
  private final Set<BlockPos> blocksToMine;

  public static Set<BlockPos> getRectangleFromTo(BlockPos from, BlockPos to) {
    var cube = new HashSet<BlockPos>();
    for (var x = Math.min(from.getX(), to.getX()); x <= Math.max(from.getX(), to.getX()); x++) {
      for (var y = Math.min(from.getY(), to.getY()); y <= Math.max(from.getY(), to.getY()); y++) {
        for (var z = Math.min(from.getZ(), to.getZ()); z <= Math.max(from.getZ(), to.getZ()); z++) {
          cube.add(new BlockPos(x, y, z));
        }
      }
    }

    return Set.copyOf(cube);
  }

  public static Set<BlockPos> getSphereRadius(BlockPos origin, int radius) {
    var sphere = new HashSet<BlockPos>();
    for (var x = -radius; x <= radius; x++) {
      for (var y = -radius; y <= radius; y++) {
        for (var z = -radius; z <= radius; z++) {
          if (Math.sqrt(x * x + y * y + z * z) <= radius) {
            sphere.add(new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z));
          }
        }
      }
    }

    return Set.copyOf(sphere);
  }

  private boolean someBlocksCanBeMined(BotConnection bot) {
    return blocksToMine.stream().anyMatch(blockPos -> {
      var blockState = bot.minecraft().level.getBlockState(blockPos);
      return SFBlockHelpers.isFullBlock(blockState) && SFBlockHelpers.isDiggable(blockState.blockType());
    });
  }

  public void start(BotConnection bot) {
    while (someBlocksCanBeMined(bot)) {
      log.info("Searching for next block to excavate");

      try {
        PathExecutor.executePathfinding(bot, new CompositeGoal(blocksToMine.stream()
          .map(BreakBlockPosGoal::new)
          .collect(Collectors.toUnmodifiableSet())), new PathConstraint(bot) {
          @Override
          public boolean canPlaceBlockPos(BlockPos pos) {
            return super.canPlaceBlockPos(pos) && !blocksToMine.contains(pos);
          }
        }).get();
      } catch (Exception e) {
        log.error("Got exception while executing path, aborting", e);
        return;
      }
    }
  }
}
