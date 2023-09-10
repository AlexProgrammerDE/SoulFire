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
package net.pistonmaster.serverwrecker.pathfinding.execution;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import org.cloudburstmc.math.vector.Vector3d;

@ToString
@RequiredArgsConstructor
public class MovementAction implements WorldAction {
    private final Vector3d position;
    private final int yawOffset;
    private boolean didLook = false;

    @Override
    public boolean isCompleted(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
        Vector3d botPosition = movementManager.getPlayerPos();
        if (botPosition.getY() != position.getY()) {
            // We want to be on the same Y level
            return false;
        }

        return botPosition.distance(position) < 0.05;
    }

    @Override
    public void tick(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();
        Vector3d botPosition = movementManager.getPlayerPos();

        float previousYaw = movementManager.getYaw();
        movementManager.lookAt(RotationOrigin.EYES, position);
        movementManager.setPitch(0);
        movementManager.setYaw(movementManager.getYaw() + yawOffset);
        float newYaw = movementManager.getYaw();

        float yawDifference = Math.abs(previousYaw - newYaw);

        // We should only set the yaw once to the server to prevent the bot looking weird due to inaccuracy
        if (didLook && yawDifference > 5) {
            movementManager.setLastSentYaw(movementManager.getYaw());
        } else {
            didLook = true;
        }

        // Don't let the bot look up or down (makes it look weird)
        movementManager.getControlState().resetAll();
        movementManager.getControlState().setForward(true);

        if (position.getY() > botPosition.getY()) {
            movementManager.getControlState().setJumping(true);
        }
    }
}
