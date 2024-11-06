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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.state.ControlState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.util.UUID;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ClientEntity extends Entity {
  private final AbilitiesData abilitiesData = new AbilitiesData();
  private final BotConnection connection;
  private final SessionDataManager dataManager;
  private final ControlState controlState;
  private boolean showReducedDebug;
  private int opPermissionLevel;
  private double lastX = 0;
  private double lastY = 0;
  private double lastZ = 0;
  private float lastYRot = 0;
  private float lastXRot = 0;
  private boolean lastOnGround = false;
  private boolean lastHorizontalCollision = false;
  private boolean wasShiftKeyDown = false;
  private boolean wasSprinting = false;
  private boolean noPhysics = false;
  private boolean wasUnderwater = false;
  private int positionReminder = 0;
  private ServerboundPlayerInputPacket lastSentInput = new ServerboundPlayerInputPacket(false, false, false, false, false, false, false);

  public ClientEntity(
    int entityId, UUID uuid, BotConnection connection, SessionDataManager dataManager, ControlState controlState,
    Level level) {
    super(entityId, uuid, EntityType.PLAYER, level, 0, 0, 0, -180, 0, -180, 0, 0, 0);
    this.connection = connection;
    this.dataManager = dataManager;
    this.controlState = controlState;
  }

  @Override
  public void tick() {
    playerTick();

    this.sendShiftKeyState();

    var keyPressPacket = this.controlState.toServerboundPlayerInputPacket();
    if (!keyPressPacket.equals(this.lastSentInput)) {
      this.connection.sendPacket(keyPressPacket);
      this.lastSentInput = keyPressPacket;
    }

    // Send position changes
    sendPositionChanges();
  }

  private void playerTick() {
    this.noPhysics = this.isSpectator();
    if (this.isSpectator()) {
      this.onGround(false);
    }

    this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
    livingEntityTick();

    var x = MathHelper.clamp(this.x(), -2.9999999E7, 2.9999999E7);
    var z = MathHelper.clamp(this.z(), -2.9999999E7, 2.9999999E7);
    if (x != this.x() || z != this.z()) {
      this.setPosition(x, this.y(), z);
    }
  }

  private void livingEntityTick() {
    super.tick();

    // this.aiStep();

    // if (this.isFallFlying()) {
    //  this.fallFlyTicks++;
    //  else {
    //  this.fallFlyTicks = 0;
    // }

    if (this.isSleeping()) {
      this.xRot(0.0F);
    }
  }

  public void sendPositionChanges() {
    sendIsSprintingIfNeeded();

    // Detect whether anything changed
    var xDiff = x - lastX;
    var yDiff = y - lastY;
    var zDiff = z - lastZ;
    var yRotDiff = (double) (yRot - lastYRot);
    var xRotDiff = (double) (xRot - lastXRot);
    var sendPos =
      MathHelper.lengthSquared(xDiff, yDiff, zDiff) > MathHelper.square(2.0E-4)
        || ++positionReminder >= 20;
    var sendRot = xRotDiff != 0.0 || yRotDiff != 0.0;
    var sendStatus = onGround != lastOnGround || horizontalCollision != lastHorizontalCollision;

    // Send position packets if changed
    if (sendPos && sendRot) {
      sendPosRot();
    } else if (sendPos) {
      sendPos();
    } else if (sendRot) {
      sendRot();
    } else if (sendStatus) {
      sendStatus();
    }
  }

  public void handleEntityEvent(EntityEvent event) {
    switch (event) {
      case PLAYER_ENABLE_REDUCED_DEBUG -> showReducedDebug = true;
      case PLAYER_DISABLE_REDUCED_DEBUG -> showReducedDebug = false;
      case PLAYER_OP_PERMISSION_LEVEL_0 -> opPermissionLevel = 0;
      case PLAYER_OP_PERMISSION_LEVEL_1 -> opPermissionLevel = 1;
      case PLAYER_OP_PERMISSION_LEVEL_2 -> opPermissionLevel = 2;
      case PLAYER_OP_PERMISSION_LEVEL_3 -> opPermissionLevel = 3;
      case PLAYER_OP_PERMISSION_LEVEL_4 -> opPermissionLevel = 4;
      default -> super.handleEntityEvent(event);
    }
  }

  @Override
  public double eyeHeight() {
    if (this.controlState.sneaking()) {
      return connection
        .protocolVersion()
        .newerThanOrEqualTo(ProtocolVersion.v1_14)
        ? 1.27F
        : 1.54F;
    } else {
      return 1.62F;
    }
  }

  public void sendPosRot() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x;
    lastY = y;
    lastZ = z;
    positionReminder = 0;

    lastYRot = yRot;
    lastXRot = xRot;

    connection.sendPacket(
      new ServerboundMovePlayerPosRotPacket(onGround, horizontalCollision, x, y, z, yRot, xRot));
  }

  public void sendPos() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x;
    lastY = y;
    lastZ = z;
    positionReminder = 0;

    connection.sendPacket(new ServerboundMovePlayerPosPacket(onGround, horizontalCollision, x, y, z));
  }

  public void sendRot() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastYRot = yRot;
    lastXRot = xRot;

    connection.sendPacket(new ServerboundMovePlayerRotPacket(onGround, horizontalCollision, yRot, xRot));
  }

  public void sendStatus() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    connection.sendPacket(new ServerboundMovePlayerStatusOnlyPacket(onGround, horizontalCollision));
  }

  private void sendShiftKeyState() {
    var sneaking = controlState.sneaking();
    if (sneaking != this.wasShiftKeyDown) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId(), sneaking
        ? PlayerState.START_SNEAKING
        : PlayerState.STOP_SNEAKING));
      this.wasShiftKeyDown = sneaking;
    }
  }

  private void sendIsSprintingIfNeeded() {
    var sprinting = controlState.sprinting();
    if (sprinting != this.wasSprinting) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId(), sprinting
        ? PlayerState.START_SPRINTING
        : PlayerState.STOP_SPRINTING));
      this.wasSprinting = sprinting;
    }
  }

  public void jump() {
    jumpTriggerTime = 7;
  }

  @Override
  public float height() {
    return this.controlState.sneaking() ? 1.5F : 1.8F;
  }

  private boolean isSpectator() {
    return false; // TODO
  }

  private boolean isSleeping() {
    return false; // TODO
  }

  public boolean isUnderWater() {
    return this.wasUnderwater;
  }
}
