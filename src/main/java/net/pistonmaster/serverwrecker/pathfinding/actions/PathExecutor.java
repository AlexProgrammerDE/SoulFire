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
package net.pistonmaster.serverwrecker.pathfinding.actions;

import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotPreTickEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PathExecutor implements EventSubscriber<BotPreTickEvent> {
    private final Queue<Action> actions;
    private final BotConnection connection;

    public PathExecutor(BotConnection connection, List<Action> actions) {
        this.actions = new ArrayBlockingQueue<>(actions.size());
        this.actions.addAll(actions);
        this.connection = connection;
    }

    @Override
    public void on(@NonNull BotPreTickEvent event) throws Throwable {
        BotConnection connection = event.connection();
        if (connection != this.connection) {
            return;
        }

        if (actions.isEmpty()) {
            unregister();
            return;
        }

        Action action = actions.peek();
        if (action == null) {
            unregister();
            return;
        }

        if (action.isCompleted(connection)) {
            actions.remove();
            System.out.println("Reached goal! " + action);

            // Directly use tick to execute next goal
            action = actions.peek();

            // If there are no more goals, stop
            if (action == null) {
                System.out.println("Finished all goals!");
                BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
                movementManager.getControlState().resetAll();
                unregister();
                return;
            }

            System.out.println("Next goal: " + action);
        }

        action.tick(connection);
    }

    public void unregister() {
        ServerWreckerAPI.unregisterListener(this);
    }
}
