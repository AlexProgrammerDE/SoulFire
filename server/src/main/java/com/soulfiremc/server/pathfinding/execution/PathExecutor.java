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
package com.soulfiremc.server.pathfinding.execution;

import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.pathfinding.graph.ProjectedLevel;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.TimeUtil;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PathExecutor implements Consumer<BotPreTickEvent> {
  private final ExecutorService executorService;
  private final Queue<WorldAction> worldActionQueue = new LinkedBlockingQueue<>();
  private final BotConnection connection;
  private final Boolean2ObjectFunction<List<WorldAction>> findPath;
  private final CompletableFuture<Void> pathCompletionFuture;
  private int totalMovements;
  private int ticks = 0;
  private int movementNumber = 1;
  // Should be true when this path should no longer be executed and not recalculated
  private boolean cancelled = false;
  private boolean registered = false;

  public PathExecutor(
    BotConnection connection,
    Boolean2ObjectFunction<List<WorldAction>> findPath,
    CompletableFuture<Void> pathCompletionFuture) {
    this.executorService = connection.executorManager().newExecutorService(connection, "PathExecutor");
    this.connection = connection;
    this.findPath = findPath;
    this.pathCompletionFuture = pathCompletionFuture;
  }

  public static void executePathfinding(BotConnection bot, GoalScorer goalScorer,
                                        CompletableFuture<Void> pathCompletionFuture) {
    var logger = bot.logger();
    var sessionDataManager = bot.sessionDataManager();
    var clientEntity = sessionDataManager.clientEntity();

    Boolean2ObjectFunction<List<WorldAction>> findPath =
      requiresRepositioning -> {
        var level = new ProjectedLevel(
          sessionDataManager.currentLevel()
            .chunks()
            .immutableCopy());
        var inventory =
          new ProjectedInventory(
            sessionDataManager.inventoryManager().playerInventory());
        var start =
          SFVec3i.fromDouble(clientEntity.pos());
        var routeFinder =
          new RouteFinder(new MinecraftGraph(sessionDataManager.tagsState(), level, inventory, true, true), goalScorer);

        logger.info("Starting calculations at: {}", start);
        var actions = routeFinder.findRoute(start, requiresRepositioning);
        logger.info("Calculated path with {} actions: {}", actions.size(), actions);

        return actions;
      };

    var pathExecutor = new PathExecutor(bot, findPath, pathCompletionFuture);
    pathExecutor.submitForPathCalculation(true);
  }

  public void submitForPathCalculation(boolean isInitial) {
    unregister();
    connection.sessionDataManager().controlState().resetAll();

    executorService.submit(() -> {
      try {
        if (cancelled) {
          return;
        }

        if (!isInitial) {
          connection.logger().info("Waiting for one second for bot to finish falling...");
          TimeUtil.waitTime(1, TimeUnit.SECONDS);
          if (cancelled) {
            return;
          }
        }

        var newActions = findPath.get(isInitial);
        if (cancelled) {
          return;
        }

        if (newActions.isEmpty()) {
          connection.logger().info("We're already at the goal!");
          return;
        }

        connection.logger().info("Found path with {} actions!", newActions.size());

        preparePath(newActions);

        // Register again
        register();
      } catch (Throwable t) {
        pathCompletionFuture.completeExceptionally(t);
      }
    });
  }

  public void preparePath(List<WorldAction> worldActions) {
    this.worldActionQueue.addAll(worldActions);
    this.totalMovements = worldActions.size();
  }

  @Override
  public void accept(BotPreTickEvent event) {
    var connection = event.connection();
    if (connection != this.connection) {
      return;
    }

    // This method should not be called if the path is cancelled
    if (!registered || cancelled) {
      return;
    }

    if (worldActionQueue.isEmpty()) {
      unregister();
      return;
    }

    var worldAction = worldActionQueue.peek();
    if (worldAction == null) {
      unregister();
      return;
    }

    if (worldAction instanceof RecalculatePathAction) {
      connection.logger().info("Recalculating path...");
      recalculatePath();
      return;
    }

    if (ticks > 0 && ticks >= worldAction.getAllowedTicks()) {
      connection.logger().warn("Took too long to complete action: {}", worldAction);
      connection.logger().warn("Recalculating path...");
      recalculatePath();
      return;
    }

    if (worldAction.isCompleted(connection)) {
      worldActionQueue.remove();
      connection
        .logger()
        .info("Reached goal {}/{} in {} ticks!", movementNumber, totalMovements, ticks);
      movementNumber++;
      ticks = 0;

      // Directly use tick to execute next goal
      worldAction = worldActionQueue.peek();

      // If there are no more goals, stop
      if (worldAction == null) {
        connection.logger().info("Finished all goals!");
        connection.sessionDataManager().controlState().resetAll();
        pathCompletionFuture.complete(null);
        unregister();
        return;
      }

      if (worldAction instanceof RecalculatePathAction) {
        connection.logger().info("Recalculating path...");
        recalculatePath();
        return;
      }

      connection.logger().debug("Next goal: {}", worldAction);
    }

    ticks++;
    worldAction.tick(connection);
  }

  public synchronized void register() {
    if (cancelled) {
      return;
    }

    if (registered) {
      return;
    }

    registered = true;
    connection.sessionDataManager().clientEntity().controlState().incrementActivelyControlling();
    EventUtil.runAndAssertChanged(
      connection.eventBus(),
      () -> connection.eventBus().registerConsumer(this, BotPreTickEvent.class));
  }

  public synchronized void unregister() {
    if (!registered) {
      return;
    }

    registered = false;
    connection.sessionDataManager().clientEntity().controlState().decrementActivelyControlling();
    EventUtil.runAndAssertChanged(
      connection.eventBus(),
      () -> connection.eventBus().unregisterConsumer(this, BotPreTickEvent.class));
  }

  public void cancel() {
    cancelled = true;
    unregister();
    pathCompletionFuture.completeExceptionally(new IllegalStateException("Path was cancelled"));
  }

  public void recalculatePath() {
    submitForPathCalculation(false);
  }
}
