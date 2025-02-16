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
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import com.soulfiremc.server.util.mcstructs.AABB;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.jetbrains.annotations.Nullable;

@Getter
@Slf4j
public class MultiPlayerGameMode {
  private final BotConnection connection;
  private final SessionDataManager dataManager;
  private GameMode localPlayerMode = GameMode.SURVIVAL;
  private GameMode previousLocalPlayerMode;
  private int carriedIndex;
  private int sequenceNumber = 0;

  public MultiPlayerGameMode(BotConnection connection, SessionDataManager dataManager) {
    this.connection = connection;
    this.dataManager = dataManager;
  }

  public void adjustPlayer(Player player) {
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }

  public void setLocalMode(LocalPlayer player, GameMode localPlayerMode, @Nullable GameMode previousLocalPlayerMode) {
    this.localPlayerMode = localPlayerMode;
    this.previousLocalPlayerMode = previousLocalPlayerMode;
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }

  public void setLocalMode(LocalPlayer player, GameMode type) {
    if (type != this.localPlayerMode) {
      this.previousLocalPlayerMode = this.localPlayerMode;
    }

    this.localPlayerMode = type;
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }

  public void tick() {
    this.ensureHasSentCarriedItem();
  }

  public void attack(Player player, Entity targetEntity) {
    this.ensureHasSentCarriedItem();
    this.connection.sendPacket(new ServerboundInteractPacket(targetEntity.entityId(), InteractAction.ATTACK, player.isShiftKeyDown()));
    if (this.localPlayerMode != GameMode.SPECTATOR) {
      player.attack(targetEntity);
      player.resetAttackStrengthTicker();
    }
  }

  private void ensureHasSentCarriedItem() {
    var i = this.dataManager.localPlayer().inventory().selected;
    if (i != this.carriedIndex) {
      this.carriedIndex = i;
      this.connection.sendPacket(new ServerboundSetCarriedItemPacket(this.carriedIndex));
    }
  }

  public void incrementSequenceNumber() {
    sequenceNumber++;
  }

  public void useItemInHand(Hand hand) {
    this.ensureHasSentCarriedItem();
    this.incrementSequenceNumber();
    var dataManager = connection.dataManager();
    connection.sendPacket(new ServerboundUseItemPacket(hand, sequenceNumber, dataManager.localPlayer().yRot(), dataManager.localPlayer().xRot()));
  }

  public void placeBlock(Hand hand, BlockPlaceAgainstData blockPlaceAgainstData) {
    placeBlock(hand, blockPlaceAgainstData.againstPos().toVector3i(), blockPlaceAgainstData.blockFace());
  }

  public void placeBlock(Hand hand, Vector3i againstBlock, BlockFace againstFace) {
    this.ensureHasSentCarriedItem();
    this.incrementSequenceNumber();
    var dataManager = connection.dataManager();
    var clientEntity = dataManager.localPlayer();
    var level = dataManager.currentLevel();

    var eyePosition = clientEntity.eyePosition();

    var againstPlacePosition = againstFace.getMiddleOfFace(SFVec3i.fromInt(againstBlock));

    clientEntity.lookAt(RotationOrigin.EYES, againstPlacePosition);
    clientEntity.sendPositionChanges();

    var viewDirection = clientEntity.getLookAngle();
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

    var insideBlock = !level.getBlockCollisionBoxes(new AABB(eyePosition, eyePosition)).isEmpty();

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
    this.ensureHasSentCarriedItem();
    this.incrementSequenceNumber();
    connection.sendPacket(
      new ServerboundPlayerActionPacket(
        PlayerAction.START_DIGGING, blockPos, direction, sequenceNumber));
  }

  public void sendEndBreakBlock(Vector3i blockPos, Direction direction) {
    this.ensureHasSentCarriedItem();
    this.incrementSequenceNumber();
    connection.sendPacket(
      new ServerboundPlayerActionPacket(
        PlayerAction.FINISH_DIGGING, blockPos, direction, sequenceNumber));
  }

  public void sendBreakBlockAnimation() {
    connection.sendPacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
  }

  public record BlockPlaceAgainstData(SFVec3i againstPos, BlockFace blockFace) {}
}
