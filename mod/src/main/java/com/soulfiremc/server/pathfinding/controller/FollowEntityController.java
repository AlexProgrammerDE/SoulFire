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
import com.soulfiremc.server.pathfinding.goals.CloseToPosGoal;
import com.soulfiremc.server.pathfinding.goals.DynamicGoalScorer;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.VectorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public final class FollowEntityController {
  private final Predicate<Entity> entityMatcher;
  private final int maxRadius;

  public void start(BotConnection bot) {
    while (true) {
      var entity = StreamSupport.stream(bot.minecraft().level.entitiesForRendering().spliterator(), false)
        .filter(entityMatcher)
        .findAny();
      if (entity.isEmpty()) {
        log.info("Entity not found, aborting");
        return;
      }

      if (VectorHelper.fromBlockPos(entity.get().blockPosition()).distance(VectorHelper.fromBlockPos(bot.minecraft().player.blockPosition())) <= maxRadius) {
        TimeUtil.waitTime(1, TimeUnit.SECONDS);
        continue;
      }

      try {
        PathExecutor.executePathfinding(bot, (DynamicGoalScorer) () -> new CloseToPosGoal(SFVec3i.fromInt(entity.get().blockPosition()), maxRadius), new PathConstraint(bot)).get();
      } catch (Exception e) {
        log.error("Got exception while executing path, aborting", e);
        return;
      }
    }
  }
}
