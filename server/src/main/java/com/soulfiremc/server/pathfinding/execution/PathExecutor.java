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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PathExecutor implements Consumer<BotPreTickEvent> {
  private final Queue<WorldAction> worldActionQueue = new LinkedBlockingQueue<>();
  private final BotConnection connection;
  private final Boolean2ObjectFunction<List<WorldAction>> findPath;
  private final CompletableFuture<Void> pathCompletionFuture;
  private int totalMovements;
  private int ticks = 0;
  private int movementNumber = 1;
  private boolean registered = false;

  public PathExecutor(
    BotConnection connection,
    Boolean2ObjectFunction<List<WorldAction>> findPath,
    CompletableFuture<Void> pathCompletionFuture) {
    this.connection = connection;
    this.findPath = findPath;
    this.pathCompletionFuture = pathCompletionFuture;
  }

  public static void executePathfinding(BotConnection bot, GoalScorer goalScorer,
                                        CompletableFuture<Void> pathCompletionFuture) {
    // Cancel the path if the bot is disconnected
    bot.shutdownHooks().add(() -> {
      if (!pathCompletionFuture.isDone()) {
        pathCompletionFuture.cancel(true);
      }
    });

    var logger = bot.logger();
    var dataManager = bot.dataManager();
    var clientEntity = dataManager.clientEntity();

    Boolean2ObjectFunction<List<WorldAction>> findPath =
      requiresRepositioning -> {
        var level = new ProjectedLevel(
          dataManager.currentLevel()
            .chunks()
            .immutableCopy());
        var inventory =
          new ProjectedInventory(
            dataManager.inventoryManager().playerInventory());
        var start =
          SFVec3i.fromDouble(clientEntity.pos());
        var routeFinder =
          new RouteFinder(new MinecraftGraph(dataManager.tagsState(), level, inventory, true, true), goalScorer);

        logger.info("Starting calculations at: {}", start);
        var actions = routeFinder.findRoute(start, requiresRepositioning, pathCompletionFuture);
        logger.info("Calculated path with {} actions: {}", actions.size(), actions);

        return actions;
      };

    var pathExecutor = new PathExecutor(bot, findPath, pathCompletionFuture);
    pathExecutor.submitForPathCalculation(true);
  }

  public boolean isDone() {
    return pathCompletionFuture.isDone();
  }

  public void submitForPathCalculation(boolean isInitial) {
    unregister();
    connection.dataManager().controlState().resetAll();

    connection.scheduler().schedule(() -> {
      try {
        if (isDone()) {
          return;
        }

        if (!isInitial) {
          connection.logger().info("Waiting for one second for bot to finish falling...");
          TimeUtil.waitTime(1, TimeUnit.SECONDS);
          if (isDone()) {
            return;
          }
        }

        var newActions = findPath.get(isInitial);
        if (isDone()) {
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
    this.worldActionQueue.clear();
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
    if (!registered || isDone()) {
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
        connection.dataManager().controlState().resetAll();
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
    if (isDone()) {
      return;
    }

    if (registered) {
      return;
    }

    registered = true;
    connection.dataManager().clientEntity().controlState().incrementActivelyControlling();
    EventUtil.runAndAssertChanged(
      connection.eventBus(),
      () -> connection.eventBus().registerConsumer(this, BotPreTickEvent.class));
  }

  public synchronized void unregister() {
    if (!registered) {
      return;
    }

    registered = false;
    connection.dataManager().clientEntity().controlState().decrementActivelyControlling();
    EventUtil.runAndAssertChanged(
      connection.eventBus(),
      () -> connection.eventBus().unregisterConsumer(this, BotPreTickEvent.class));
  }

  public void cancel() {
    if (!isDone()) {
      pathCompletionFuture.cancel(true);
    }

    unregister();
  }

  public void recalculatePath() {
    submitForPathCalculation(false);
  }
}
