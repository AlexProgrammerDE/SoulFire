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
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.TimeUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FollowEntityController {
  private final int entityId;
  private final int maxRadius;

  public void start(BotConnection bot) {
    while (true) {
      var entity = bot.dataManager().entityTrackerState().getEntity(entityId);
      if (entity == null) {
        log.info("Entity not found, aborting");
        return;
      }

      if (entity.blockPos().distance(bot.dataManager().clientEntity().blockPos()) <= maxRadius) {
        TimeUtil.waitTime(1, TimeUnit.SECONDS);
        continue;
      }

      var pathFuture = new CompletableFuture<Void>();
      PathExecutor.executePathfinding(bot, new CloseToPosGoal(SFVec3i.fromInt(entity.blockPos()), maxRadius),
        pathFuture);

      try {
        pathFuture.get();
      } catch (Exception e) {
        log.error("Got exception while executing path, aborting", e);
        return;
      }
    }
  }
}
