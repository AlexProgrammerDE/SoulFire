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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.util.MathHelper;
import net.pistonmaster.soulfire.server.util.VectorHelper;

@Slf4j
@RequiredArgsConstructor
public final class GapJumpAction implements WorldAction {
    private final SWVec3i blockPosition;
    private boolean didLook = false;
    private boolean lockYaw = false;
    private int noJumpTicks = 0;

    @Override
    public boolean isCompleted(BotConnection connection) {
        var clientEntity = connection.sessionDataManager().clientEntity();
        var botPosition = clientEntity.pos();
        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        var blockMeta = levelState.getBlockStateAt(blockPosition);
        var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);
        if (MathHelper.isOutsideTolerance(botPosition.getY(), targetMiddleBlock.getY(), 0.2)) {
            // We want to be on the same Y level
            return false;
        } else {
            var distance = botPosition.distance(targetMiddleBlock);
            return distance <= 0.3;
        }
    }

    @Override
    public void tick(BotConnection connection) {
        var clientEntity = connection.sessionDataManager().clientEntity();
        clientEntity.controlState().resetAll();

        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return;
        }

        var blockMeta = levelState.getBlockStateAt(blockPosition);
        var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);

        var previousYaw = clientEntity.yaw();
        clientEntity.lookAt(RotationOrigin.EYES, targetMiddleBlock);
        clientEntity.pitch(0);
        var newYaw = clientEntity.yaw();

        var yawDifference = Math.abs(MathHelper.wrapDegrees(newYaw - previousYaw));

        // We should only set the yaw once to the server to prevent the bot looking weird due to inaccuracy
        if (!didLook) {
            didLook = true;
        } else if (yawDifference > 5 || lockYaw) {
            lockYaw = true;
            clientEntity.lastYaw(newYaw);
        }

        clientEntity.controlState().forward(true);

        if (shouldJump()) {
            clientEntity.controlState().jumping(true);
        }
    }

    private boolean shouldJump() {
        if (noJumpTicks < 1) {
            noJumpTicks++;
            return false;
        } else {
            noJumpTicks = 0;
            return true;
        }
    }

    @Override
    public int getAllowedTicks() {
        // 5-seconds max to walk to a block
        return 5 * 20;
    }

    @Override
    public String toString() {
        return "GapJumpAction -> " + blockPosition.formatXYZ();
    }
}
