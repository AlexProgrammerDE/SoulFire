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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import net.pistonmaster.serverwrecker.pathfinding.BotWorldState;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import org.cloudburstmc.math.vector.Vector3d;

public record MovementAction(BotWorldState worldState) implements Action {
    @Override
    public boolean isCompleted(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
        Vector3d botPosition = movementManager.getPlayerPos();
        double distanceToGoal = botPosition.distance(worldState.position());

        return distanceToGoal < 0.5;
    }

    @Override
    public void tick(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
        Vector3d botPosition = movementManager.getPlayerPos();

        movementManager.lookAt(RotationOrigin.EYES, worldState.position());
        movementManager.setRotation(movementManager.getYaw(), 0);

        // Don't let the bot look up or down (makes it look weird)
        movementManager.getControlState().resetAll();
        movementManager.getControlState().setForward(true);

        if (worldState.position().getY() > botPosition.getY()) {
            movementManager.getControlState().setJumping(true);
        }
    }
}
