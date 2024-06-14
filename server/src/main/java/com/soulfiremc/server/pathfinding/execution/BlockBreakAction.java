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

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.BlockTypeHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;

@Slf4j
@RequiredArgsConstructor
public final class BlockBreakAction implements WorldAction {
  @Getter
  private final SFVec3i blockPosition;
  private final BlockFace blockBreakSideHint;
  boolean finishedDigging = false;
  private boolean didLook = false;
  private boolean putInHand = false;
  private int remainingTicks = -1;
  private int totalTicks = -1;

  public BlockBreakAction(MovementMiningCost movementMiningCost) {
    this(movementMiningCost.block(), movementMiningCost.blockBreakSideHint());
  }

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.dataManager().currentLevel();

    return BlockTypeHelper.isEmptyBlock(level.getBlockState(blockPosition).blockType());
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return SFVec3i.fromDouble(connection.dataManager().clientEntity().pos());
  }

  @Override
  public void tick(BotConnection connection) {
    var dataManager = connection.dataManager();
    var clientEntity = dataManager.clientEntity();
    dataManager.controlState().resetAll();

    var level = dataManager.currentLevel();
    if (!didLook) {
      didLook = true;
      var previousYaw = clientEntity.yaw();
      var previousPitch = clientEntity.pitch();
      clientEntity.lookAt(
        RotationOrigin.EYES,
        blockBreakSideHint.getMiddleOfFace(blockPosition));
      if (previousPitch != clientEntity.pitch() || previousYaw != clientEntity.yaw()) {
        clientEntity.sendRot();
      }
    }

    if (!putInHand) {
      if (ItemPlaceHelper.placeBestToolInHand(dataManager, blockPosition)) {
        putInHand = true;
      }

      return;
    }

    if (finishedDigging) {
      return;
    }

    if (remainingTicks == -1) {
      var optionalBlockType = level.getBlockState(blockPosition).blockType();
      if (optionalBlockType == BlockType.VOID_AIR) {
        log.warn("Block at {} is not in view range!", blockPosition);
        return;
      }

      remainingTicks = totalTicks =
        Costs.getRequiredMiningTicks(
            dataManager.tagsState(),
            dataManager.clientEntity(),
            dataManager.inventoryManager().playerInventory(),
            clientEntity.onGround(),
            dataManager.inventoryManager().playerInventory().getHeldItem().item(),
            optionalBlockType)
          .ticks();
      dataManager.botActionManager()
        .sendStartBreakBlock(blockPosition.toVector3i(), blockBreakSideHint.toDirection());

      // We instamine or are in creative mode
      // In that case don't send finish and no swing animation
      if (remainingTicks == 0) {
        finishedDigging = true;

        // Predict state change
        // This only happens with instamine
        dataManager.currentLevel().setBlock(blockPosition.toVector3i(), BlockState.forDefaultBlockType(BlockType.AIR));
      }
    } else if (--remainingTicks == 0) {
      dataManager.botActionManager()
        .sendEndBreakBlock(blockPosition.toVector3i(), blockBreakSideHint.toDirection());
      finishedDigging = true;
    } else {
      dataManager.botActionManager().sendBreakBlockAnimation();
    }
  }

  @Override
  public int getAllowedTicks() {
    return totalTicks == -1 ? 20 : totalTicks + 20;
  }

  @Override
  public String toString() {
    return "BlockBreakAction -> " + blockPosition.formatXYZ();
  }
}
