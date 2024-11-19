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
package com.soulfiremc.server.protocol.bot;

import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.mcstructs.AABB;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

/**
 * Manages mostly block and interaction related stuff that requires to keep track of sequence
 * numbers.
 */
@Slf4j
@Data
@RequiredArgsConstructor
public class BotActionManager {
  @ToString.Exclude
  private final BotConnection connection;
  private int sequenceNumber = 0;

  public void incrementSequenceNumber() {
    sequenceNumber++;
  }

  public void useItemInHand(Hand hand) {
    incrementSequenceNumber();
    var dataManager = connection.dataManager();
    connection.sendPacket(new ServerboundUseItemPacket(hand, sequenceNumber, dataManager.localPlayer().yRot(), dataManager.localPlayer().xRot()));
  }

  public void placeBlock(Hand hand, BlockPlaceAgainstData blockPlaceAgainstData) {
    placeBlock(hand, blockPlaceAgainstData.againstPos().toVector3i(), blockPlaceAgainstData.blockFace());
  }

  public void placeBlock(Hand hand, Vector3i againstBlock, BlockFace againstFace) {
    incrementSequenceNumber();
    var dataManager = connection.dataManager();
    var clientEntity = dataManager.localPlayer();
    var level = dataManager.currentLevel();

    var eyePosition = clientEntity.eyePosition();

    var againstPlacePosition = againstFace.getMiddleOfFace(SFVec3i.fromInt(againstBlock));

    clientEntity.lookAt(RotationOrigin.EYES, againstPlacePosition);
    clientEntity.sendPositionChanges();

    var viewDirection = clientEntity.getViewVector();
    var blockInteractionRange = clientEntity.attributeValue(AttributeType.BLOCK_INTERACTION_RANGE);
    var endPos = eyePosition.add(
      viewDirection.getX() * blockInteractionRange,
      viewDirection.getY() * blockInteractionRange,
      viewDirection.getZ() * blockInteractionRange
    );
    var againstState = level.getBlockState(againstBlock);
    var hitResult = AABB.clip(againstState.getCollisionBoxes(againstBlock), eyePosition, endPos, againstBlock);
    if (hitResult == null) {
      log.warn("Failed to place block at {} against {}", againstBlock, againstFace);
      return;
    }

    var insideBlock = !level.getCollisionBoxes(new AABB(eyePosition, eyePosition)).isEmpty();

    var blockPlacePosition = hitResult.getVector3i();
    var blockPlaceLocation = hitResult.location();

    connection.sendPacket(
      new ServerboundUseItemOnPacket(
        hitResult.getVector3i(),
        againstFace.toDirection(),
        hand,
        (float) (blockPlaceLocation.getX() - (double) blockPlacePosition.getX()),
        (float) (blockPlaceLocation.getY() - (double) blockPlacePosition.getY()),
        (float) (blockPlaceLocation.getZ() - (double) blockPlacePosition.getZ()),
        insideBlock,
        false,
        sequenceNumber));
  }

  public void sendStartBreakBlock(Vector3i blockPos, Direction direction) {
    incrementSequenceNumber();
    connection.sendPacket(
      new ServerboundPlayerActionPacket(
        PlayerAction.START_DIGGING, blockPos, direction, sequenceNumber));
  }

  public void sendEndBreakBlock(Vector3i blockPos, Direction direction) {
    incrementSequenceNumber();
    connection.sendPacket(
      new ServerboundPlayerActionPacket(
        PlayerAction.FINISH_DIGGING, blockPos, direction, sequenceNumber));
  }

  public void sendBreakBlockAnimation() {
    connection.sendPacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
  }

  public record BlockPlaceAgainstData(SFVec3i againstPos, BlockFace blockFace) {}
}
