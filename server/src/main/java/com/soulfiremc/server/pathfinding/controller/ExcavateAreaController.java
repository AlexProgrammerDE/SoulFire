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

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.BreakBlockPosGoal;
import com.soulfiremc.server.pathfinding.goals.CompositeGoal;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class ExcavateAreaController {
  private final Set<SFVec3i> blocksToMine;

  public static Set<SFVec3i> getRectangleFromTo(SFVec3i from, SFVec3i to) {
    var cube = new HashSet<SFVec3i>();
    for (var x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++) {
      for (var y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++) {
        for (var z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
          cube.add(new SFVec3i(x, y, z));
        }
      }
    }

    return Set.copyOf(cube);
  }

  public static Set<SFVec3i> getSphereRadius(SFVec3i origin, int radius) {
    var sphere = new HashSet<SFVec3i>();
    for (var x = -radius; x <= radius; x++) {
      for (var y = -radius; y <= radius; y++) {
        for (var z = -radius; z <= radius; z++) {
          if (Math.sqrt(x * x + y * y + z * z) <= radius) {
            sphere.add(new SFVec3i(origin.x + x, origin.y + y, origin.z + z));
          }
        }
      }
    }

    return Set.copyOf(sphere);
  }

  private boolean someBlocksCanBeMined(BotConnection bot) {
    return blocksToMine.stream().anyMatch(blockPos -> {
      var blockState = bot.dataManager().currentLevel().getBlockState(blockPos);
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
          public boolean canPlaceBlockPos(SFVec3i pos) {
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
