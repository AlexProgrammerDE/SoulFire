/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.pathfinding.goals;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotPreTickEvent;
import net.pistonmaster.serverwrecker.pathfinding.BlockPosition;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PathExecutor implements EventSubscriber<BotPreTickEvent> {
    private final Queue<BlockPosition> goals;
    private final BotConnection connection;

    public PathExecutor(BotConnection connection, List<BlockPosition> goals) {
        this.goals = new ArrayBlockingQueue<>(goals.size());
        this.goals.addAll(goals);
        this.connection = connection;
    }

    @Override
    public void on(@NonNull BotPreTickEvent event) throws Throwable {
        BotConnection connection = event.connection();
        if (connection != this.connection) {
            return;
        }

        if (goals.isEmpty()) {
            unregister();
            return;
        }

        BlockPosition goal = goals.peek();
        if (goal == null) {
            unregister();
            return;
        }

        Vector3d goalPosition = goal.position();

        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
        Vector3d botPosition = movementManager.getPlayerPos();

        double distanceToGoal = botPosition.distance(goal.position());
        if (distanceToGoal < 0.2) {
            goals.remove();
            System.out.println("Reached goal! " + goal);

            // Directly use tick to execute next goal
            goal = goals.peek();

            // If there are no more goals, stop
            if (goal == null) {
                System.out.println("Finished all goals!");
                movementManager.getControlState().resetAll();
                unregister();
                return;
            }

            System.out.println("Next goal: " + goal);

            goalPosition = goal.position();
        }

        movementManager.getControlState().resetAll();
        movementManager.lookAt(RotationOrigin.FEET, goalPosition);
        movementManager.getControlState().setForward(true);

        if (goalPosition.getY() > botPosition.getY()) {
            movementManager.getControlState().setJumping(true);
        }
    }

    public void unregister() {
        ServerWreckerAPI.unregisterListener(this);
    }
}
