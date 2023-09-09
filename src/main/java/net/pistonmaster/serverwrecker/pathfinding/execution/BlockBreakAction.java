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
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import org.cloudburstmc.math.vector.Vector3i;

@ToString
@RequiredArgsConstructor
public class BlockBreakAction implements WorldAction {
    private final Vector3i blockPosition;
    private boolean didLook = false;

    @Override
    public boolean isCompleted(BotConnection connection) {
        LevelState levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        return levelState.getBlockTypeAt(blockPosition).map(blockType -> blockType.blockShapeTypes().isEmpty()).orElse(false);
    }

    @Override
    public void tick(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();

        if (!didLook) {
            didLook = true;
            movementManager.lookAt(RotationOrigin.EYES, blockPosition.toDouble());
        }

        // TODO: Break block
    }
}
