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
package com.soulfiremc.server.pathfinding.graph.actions;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.PathfindingGraph;
import com.soulfiremc.server.pathfinding.world.BlockState;

/// Consumer interface for registering block subscriptions during movement setup.
@FunctionalInterface
public interface SubscriptionConsumer {
  void subscribe(SFVec3i offset, MovementSubscription<?> subscription);

  /// Subscription interface for processing blocks during pathfinding.
  interface MovementSubscription<M extends GraphAction> {
    SubscriptionSingleResult processBlock(
      PathfindingGraph graph,
      SFVec3i key,
      M action,
      BlockState blockState,
      SFVec3i absoluteKey);

    @SuppressWarnings("unchecked")
    default SubscriptionSingleResult processBlockUnsafe(
      PathfindingGraph graph,
      SFVec3i key,
      GraphAction action,
      BlockState blockState,
      SFVec3i absolutePositionBlock) {
      return processBlock(graph, key, (M) action, blockState, absolutePositionBlock);
    }
  }

  /// Result of processing a single block subscription.
  enum SubscriptionSingleResult {
    CONTINUE,
    IMPOSSIBLE
  }
}
