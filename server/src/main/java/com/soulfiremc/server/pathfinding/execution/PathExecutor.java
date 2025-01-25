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

import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.TimeUtil;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class PathExecutor implements ControllingTask {
  private static final int MAX_ERROR_DISTANCE = 20;
  private final Queue<WorldAction> worldActionQueue = new LinkedBlockingQueue<>();
  private final BotConnection connection;
  private final Boolean2ObjectFunction<List<WorldAction>> findPath;
  private final CompletableFuture<Void> pathCompletionFuture;
  private int totalMovements;
  private int ticks = 0;
  private int movementNumber = 1;

  public PathExecutor(
    BotConnection connection,
    Boolean2ObjectFunction<List<WorldAction>> findPath,
    CompletableFuture<Void> pathCompletionFuture) {
    this.connection = connection;
    this.findPath = findPath;
    this.pathCompletionFuture = pathCompletionFuture;
  }

  public static CompletableFuture<Void> executePathfinding(BotConnection bot, GoalScorer goalScorer, PathConstraint pathConstraint) {
    var logger = bot.logger();
    var dataManager = bot.dataManager();
    var clientEntity = dataManager.localPlayer();

    Boolean2ObjectFunction<List<WorldAction>> findPath =
      requiresRepositioning -> {
        var level = dataManager.currentLevel()
          .chunks()
          .immutableCopy();
        var inventory =
          new ProjectedInventory(bot.inventoryManager().playerInventory(), dataManager.localPlayer(), dataManager.tagsState(), pathConstraint);
        var start =
          SFVec3i.fromDouble(clientEntity.pos());
        var startBlockState = level.getBlockState(start);
        if (SFBlockHelpers.isTopFullBlock(startBlockState.collisionShape())) {
          // If the player is inside a block, move them up
          start = start.add(0, 1, 0);
        }

        var routeFinder =
          new RouteFinder(new MinecraftGraph(dataManager.tagsState(), level, inventory, pathConstraint), goalScorer);

        logger.info("Starting calculations at: {}", start.formatXYZ());
        var actionsFuture = routeFinder.findRouteFuture(NodeState.forInfo(start, inventory), requiresRepositioning);
        bot.shutdownHooks().add(() -> actionsFuture.cancel(true));
        var actions = actionsFuture.join();
        logger.info("Calculated path with {} actions: {}", actions.size(), actions);

        return actions;
      };

    var pathCompletionFuture = new CompletableFuture<Void>();

    // Cancel the path if the bot is disconnected
    bot.shutdownHooks().add(() -> pathCompletionFuture.cancel(true));

    var pathExecutor = new PathExecutor(bot, findPath, pathCompletionFuture);
    pathExecutor.submitForPathCalculation(true);

    return pathCompletionFuture;
  }

  @Override
  public boolean isDone() {
    return pathCompletionFuture.isDone();
  }

  public void submitForPathCalculation(boolean isInitial) {
    unregister();

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
        log.error("Error while calculating path", t);
        pathCompletionFuture.completeExceptionally(t);
      }
    });
  }

  public void preparePath(List<WorldAction> worldActions) {
    this.worldActionQueue.clear();
    this.worldActionQueue.addAll(worldActions);
    this.totalMovements = worldActions.size();
    this.ticks = 0;
    this.movementNumber = 1;
  }

  @Override
  public void tick() {
    // This method should not be called if the path is cancelled
    if (isDone()) {
      unregister();
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

    if (SFVec3i.fromDouble(connection.dataManager().localPlayer().pos())
      .distance(worldAction.targetPosition(connection)) > MAX_ERROR_DISTANCE) {
      connection.logger().warn("More than {} blocks away from target, this must be a mistake!", MAX_ERROR_DISTANCE);
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
        connection.controlState().resetAll();
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

  @Override
  public void stop() {
    if (!isDone()) {
      pathCompletionFuture.cancel(true);
    }

    unregister();
  }

  public synchronized void register() {
    if (isDone()) {
      return;
    }

    connection.botControl().registerControllingTask(this);
  }

  public synchronized void unregister() {
    connection.botControl().unregisterControllingTask(this);
    connection.controlState().resetAll();
  }

  public void recalculatePath() {
    submitForPathCalculation(false);
  }
}
