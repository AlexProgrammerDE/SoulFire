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
package net.pistonmaster.soulfire.server.pathfinding.execution;

import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import net.pistonmaster.soulfire.server.api.event.EventUtil;
import net.pistonmaster.soulfire.server.api.event.bot.BotPreTickEvent;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.util.TimeUtil;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PathExecutor implements Consumer<BotPreTickEvent> {
    private final Queue<WorldAction> worldActions;
    private final BotConnection connection;
    private final Boolean2ObjectFunction<List<WorldAction>> findPath;
    private final ExecutorService executorService;
    private final int totalMovements;
    private int ticks = 0;
    private int movementNumber;
    private boolean cancelled = false;

    public PathExecutor(BotConnection connection, List<WorldAction> worldActions, Boolean2ObjectFunction<List<WorldAction>> findPath,
                        ExecutorService executorService) {
        this.worldActions = new ArrayBlockingQueue<>(worldActions.size());
        this.worldActions.addAll(worldActions);
        this.connection = connection;
        this.findPath = findPath;
        this.executorService = executorService;
        this.totalMovements = worldActions.size();
        this.movementNumber = 1;
    }

    @Override
    public void accept(BotPreTickEvent event) {
        var connection = event.connection();
        if (connection != this.connection) {
            return;
        }

        if (worldActions.isEmpty()) {
            unregister();
            return;
        }

        var worldAction = worldActions.peek();
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
            worldActions.remove();
            connection.logger().info("Reached goal {}/{} in {} ticks!", movementNumber, totalMovements, ticks);
            movementNumber++;
            ticks = 0;

            // Directly use tick to execute next goal
            worldAction = worldActions.peek();

            // If there are no more goals, stop
            if (worldAction == null) {
                connection.logger().info("Finished all goals!");
                connection.sessionDataManager().controlState().resetAll();
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

    public void register() {
        EventUtil.runAndAssertChanged(connection.eventBus(), () ->
                connection.eventBus().registerConsumer(this, BotPreTickEvent.class));
    }

    public void unregister() {
        EventUtil.runAndAssertChanged(connection.eventBus(), () ->
                connection.eventBus().unregisterConsumer(this, BotPreTickEvent.class));
    }

    public void cancel() {
        cancelled = true;
    }

    private void recalculatePath() {
        this.unregister();
        connection.sessionDataManager().controlState().resetAll();

        executorService.submit(() -> {
            try {
                if (cancelled) {
                    return;
                }

                connection.logger().info("Waiting for one second for bot to finish falling...");
                TimeUtil.waitTime(1, TimeUnit.SECONDS);
                if (cancelled) {
                    return;
                }

                var newActions = findPath.get(false);
                if (cancelled) {
                    return;
                }

                if (newActions.isEmpty()) {
                    connection.logger().info("We're already at the goal!");
                    return;
                }

                connection.logger().info("Found new path with {} actions!", newActions.size());
                var newExecutor = new PathExecutor(connection, newActions, findPath, executorService);
                newExecutor.register();
            } catch (Throwable t) {
                connection.logger().error("Failed to recalculate path!", t);
            }
        });
    }
}
