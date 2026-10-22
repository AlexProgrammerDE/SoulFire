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
package com.soulfiremc.server.pathfinding.execution;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class PathExecutorLifecycleTest {
  @Test
  void finishedPathsReleaseTheirShutdownRegistrations() {
    var bot = mock(BotConnection.class);
    var hooks = new CopyOnWriteArrayList<Runnable>();
    when(bot.shutdownHooks()).thenReturn(hooks);

    for (var attempt = 0; attempt < 30; attempt++) {
      var path = PathExecutor.createPathfinding(bot, mock(GoalScorer.class), mock(PathConstraint.class));
      assertEquals(1, hooks.size());
      switch (attempt % 3) {
        case 0 -> path.completion().complete(null);
        case 1 -> path.completion().completeExceptionally(new IllegalStateException("No route"));
        case 2 -> path.completion().cancel(true);
      }
      assertTrue(hooks.isEmpty());
    }
  }

  @Test
  void shutdownCancelsPendingPathsWithoutRemovingOtherRegistrations() {
    var bot = mock(BotConnection.class);
    var hooks = new CopyOnWriteArrayList<Runnable>();
    when(bot.shutdownHooks()).thenReturn(hooks);
    Runnable other = () -> {};
    hooks.add(other);
    var first = PathExecutor.createPathfinding(bot, mock(GoalScorer.class), mock(PathConstraint.class));
    var second = PathExecutor.createPathfinding(bot, mock(GoalScorer.class), mock(PathConstraint.class));

    hooks.forEach(Runnable::run);

    assertTrue(first.completion().isCancelled());
    assertTrue(second.completion().isCancelled());
    assertEquals(List.of(other), hooks);
  }
}
