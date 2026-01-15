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

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.BreakBlockPosGoal;
import com.soulfiremc.server.pathfinding.goals.CompositeGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class CollectBlockController {
  private final Predicate<Block> blockTypeChecker;
  private final int requestedAmount;
  private final int maxRadius;
  private int collectedAmount;

  public static Set<SFVec3i> searchWithinRadius(BotConnection botConnection, Predicate<BlockState> checker, int radius) {
    var clientEntity = botConnection.minecraft().player;
    var level = botConnection.minecraft().level;
    var rootPosition = SFVec3i.fromInt(clientEntity.blockPosition());

    var minY = Math.max(level.getMinY(), rootPosition.y - radius);
    var maxY = Math.min(level.getMaxY(), rootPosition.y + radius);
    var list = new HashSet<SFVec3i>();
    for (var x = -radius; x <= radius; x++) {
      for (var z = -radius; z <= radius; z++) {
        var blockX = rootPosition.x + x;
        var blockZ = rootPosition.z + z;
        for (var y = minY; y <= maxY; y++) {
          var blockPos = new SFVec3i(blockX, y, blockZ);
          var blockState = level.getBlockState(blockPos.toBlockPos());
          if (!(blockState.getBlock() instanceof AirBlock) && checker.test(blockState)) {
            list.add(blockPos);
          }
        }
      }
    }

    return list;
  }

  public void start(BotConnection bot) {
    while (collectedAmount < requestedAmount) {
      log.info("Collecting block {}/{}", collectedAmount, requestedAmount);

      log.info("Searching for block within radius {}", maxRadius);
      var blockPos = searchWithinRadius(bot, blockState -> blockTypeChecker.test(blockState.getBlock()), maxRadius);

      if (blockPos.isEmpty()) {
        throw new IllegalStateException("Could not find matching block within radius " + maxRadius);
      }

      log.info("Found {} possible blocks to collect", blockPos.size());

      try {
        PathExecutor.executePathfinding(bot, new CompositeGoal(blockPos.stream().map(BreakBlockPosGoal::new).collect(Collectors.toUnmodifiableSet())), new PathConstraintImpl(bot)).get();
        collectedAmount++;
      } catch (Exception e) {
        log.error("Got exception while executing path, aborting", e);
        return;
      }
    }
  }
}
