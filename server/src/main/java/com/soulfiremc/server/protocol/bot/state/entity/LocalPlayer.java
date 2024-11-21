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

import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LocalPlayer extends AbstractClientPlayer {
  private final BotConnection connection;
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
  private boolean crouching;
  private int positionReminder = 0;
  private ServerboundPlayerInputPacket lastSentInput = new ServerboundPlayerInputPacket(false, false, false, false, false, false, false);

  public LocalPlayer(BotConnection connection, Level level, GameProfile gameProfile) {
    super(connection, level, gameProfile);
    this.connection = connection;
    uuid(gameProfile.getId());
  }

  @Override
  public void tick() {
    super.tick();

    this.sendShiftKeyState();

    var keyPressPacket = connection.controlState().toServerboundPlayerInputPacket();
    if (!keyPressPacket.equals(this.lastSentInput)) {
      this.connection.sendPacket(keyPressPacket);
      this.lastSentInput = keyPressPacket;
    }

    // Send position changes
    sendPositionChanges();
  }

  public void sendPositionChanges() {
    sendIsSprintingIfNeeded();

    // Detect whether anything changed
    var xDiff = x() - lastX;
    var yDiff = y() - lastY;
    var zDiff = z() - lastZ;
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

  public void sendPosRot() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x();
    lastY = y();
    lastZ = z();
    positionReminder = 0;

    lastYRot = yRot;
    lastXRot = xRot;

    connection.sendPacket(
      new ServerboundMovePlayerPosRotPacket(onGround, horizontalCollision, x(), y(), z(), yRot, xRot));
  }

  public void sendPos() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x();
    lastY = y();
    lastZ = z();
    positionReminder = 0;

    connection.sendPacket(new ServerboundMovePlayerPosPacket(onGround, horizontalCollision, x(), y(), z()));
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
    var sneaking = connection.controlState().sneaking();
    if (sneaking != this.wasShiftKeyDown) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId(), sneaking
        ? PlayerState.START_SNEAKING
        : PlayerState.STOP_SNEAKING));
      this.wasShiftKeyDown = sneaking;
    }
  }

  private void sendIsSprintingIfNeeded() {
    var sprinting = connection.controlState().sprinting();
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
  public boolean isUnderWater() {
    return this.wasUnderwater;
  }

  @Override
  public boolean isShiftKeyDown() {
    return connection.controlState().sneaking();
  }

  @Override
  public boolean isCrouching() {
    return this.crouching;
  }

  @Override
  protected boolean isHorizontalCollisionMinor(Vector3d deltaMovement) {
    var f = this.yRot() * (float) (Math.PI / 180.0);
    var d = (double) MathHelper.sin(f);
    var e = (double) MathHelper.cos(f);
    var g = (double) this.xxa * e - (double) this.zza * d;
    var h = (double) this.zza * e + (double) this.xxa * d;
    var i = MathHelper.square(g) + MathHelper.square(h);
    var j = MathHelper.square(deltaMovement.getX()) + MathHelper.square(deltaMovement.getZ());
    if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
      var k = g * deltaMovement.getX() + h * deltaMovement.getZ();
      var l = Math.acos(k / Math.sqrt(i * j));
      return l < 0.13962634F;
    } else {
      return false;
    }
  }

  public boolean isMovingSlowly() {
    return this.isCrouching() || this.isVisuallyCrawling();
  }
}
