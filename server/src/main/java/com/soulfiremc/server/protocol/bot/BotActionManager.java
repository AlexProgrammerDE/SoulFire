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

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.movement.AABB;
import java.util.ArrayList;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
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
@Data
@RequiredArgsConstructor
public class BotActionManager {
  @ToString.Exclude
  private final SessionDataManager dataManager;
  @ToString.Exclude
  private final BotConnection connection;
  private int sequenceNumber = 0;

  private static Optional<Vector3f> rayCastToBlock(
    BlockState blockState, Vector3d eyePosition, Vector3d headRotation, Vector3i targetBlock) {
    var intersections = new ArrayList<Vector3f>();

    for (var shape : blockState.getCollisionBoxes(targetBlock)) {
      shape
        .getIntersection(eyePosition, headRotation)
        .map(Vector3d::toFloat)
        .ifPresent(intersections::add);
    }

    if (intersections.isEmpty()) {
      return Optional.empty();
    }

    Vector3f closestIntersection = null;
    var closestDistance = Double.MAX_VALUE;

    for (var intersection : intersections) {
      double distance =
        intersection.distance(eyePosition.getX(), eyePosition.getY(), eyePosition.getZ());

      if (distance < closestDistance) {
        closestIntersection = intersection;
        closestDistance = distance;
      }
    }

    assert closestIntersection != null;
    return Optional.of(closestIntersection);
  }

  public void incrementSequenceNumber() {
    sequenceNumber++;
  }

  public void useItemInHand(Hand hand) {
    incrementSequenceNumber();
    connection.sendPacket(new ServerboundUseItemPacket(hand, sequenceNumber, dataManager.clientEntity().yaw(), dataManager.clientEntity().pitch()));
  }

  public void placeBlock(Hand hand, BlockPlaceAgainstData blockPlaceAgainstData) {
    placeBlock(hand, blockPlaceAgainstData.againstPos().toVector3i(), blockPlaceAgainstData.blockFace());
  }

  public void placeBlock(Hand hand, Vector3i againstBlock, BlockFace againstFace) {
    incrementSequenceNumber();
    var clientEntity = dataManager.clientEntity();
    var level = dataManager.currentLevel();

    var eyePosition = clientEntity.eyePosition();

    var againstPlacePosition = againstFace.getMiddleOfFace(SFVec3i.fromInt(againstBlock));

    var previousYaw = clientEntity.yaw();
    var previousPitch = clientEntity.pitch();
    clientEntity.lookAt(RotationOrigin.EYES, againstPlacePosition);
    if (previousPitch != clientEntity.pitch() || previousYaw != clientEntity.yaw()) {
      clientEntity.sendRot();
    }

    var rayCast =
      rayCastToBlock(
        level.getBlockState(againstBlock),
        eyePosition,
        clientEntity.rotationVector(),
        againstBlock);
    if (rayCast.isEmpty()) {
      return;
    }

    var rayCastPosition = rayCast.get().sub(againstBlock.toFloat());
    var insideBlock = !level.getCollisionBoxes(new AABB(eyePosition, eyePosition)).isEmpty();

    connection.sendPacket(
      new ServerboundUseItemOnPacket(
        againstBlock,
        againstFace.toDirection(),
        hand,
        rayCastPosition.getX(),
        rayCastPosition.getY(),
        rayCastPosition.getZ(),
        insideBlock,
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
