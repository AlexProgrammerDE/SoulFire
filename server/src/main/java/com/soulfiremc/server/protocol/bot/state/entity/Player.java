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

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Player extends LivingEntity {
  private final BotConnection connection;
  private final SessionDataManager dataManager;
  private boolean showReducedDebug;
  private int opPermissionLevel;
  private double lastX = 0;
  private double lastY = 0;
  private double lastZ = 0;
  private float lastYaw = 0;
  private float lastPitch = 0;
  private boolean lastOnGround = false;
  private int positionReminder = 0;

  public Player(
    int entityId, UUID uuid, BotConnection connection, SessionDataManager dataManager, Level level) {
    super(entityId, uuid, EntityType.PLAYER, level, 0, 0, 0, -180, 0, -180, 0, 0, 0);
    this.connection = connection;
    this.dataManager = dataManager;
  }

  @Override
  public void tick() {
    this.noPhysics = this.isSpectator();
    if (this.isSpectator()) {
      this.onGround(false);
    }

    this.updateIsUnderwater();
    super.tick();

    int i = 29999999;
    double d = MathHelper.doubleClamp(this.x(), -i, i);
    double e = MathHelper.doubleClamp(this.z(), -i, i);
    if (d != this.x() || e != this.z()) {
      this.setPos(d, this.y(), e);
    }

    ++this.attackStrengthTicker;

    this.cooldowns.tick();
    this.updatePlayerPose();

    // Send position changes
    sendPositionChanges();
  }

  @Override
  protected float getMaxHeadRotationRelativeToBody() {
    return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
  }

  private boolean isBlocking() {
    return false; // TODO: Implement
  }

  @Override
  public void aiStep() {
    if (this.jumpTriggerTime > 0) {
      --this.jumpTriggerTime;
    }

    this.inventory.tick();
    super.aiStep();
    this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    float f;
    if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
      f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
    } else {
      f = 0.0F;
    }
  }

  @Override
  public void travel(Vector3d travelVector) {
    if (this.isSwimming() && !this.isPassenger()) {
      double d = this.getLookAngle().y;
      double e = d < -0.2 ? 0.085 : 0.06;
      if (d <= 0.0
        || this.jumping
        || !this.level().getBlockState(Vector3i.from(this.x(), this.y() + 1.0 - 0.1, this.z())).getFluidState().isEmpty()) {
        Vector3d vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.add(0.0, (d - vec3.getY()) * e, 0.0));
      }
    }

    if (dataManager.abilitiesData().flying() && !this.isPassenger()) {
      double d = this.getDeltaMovement().getY();
      super.travel(travelVector);
      Vector3d vec32 = this.getDeltaMovement();
      this.setDeltaMovement(vec32.getX(), d * 0.6, vec32.getZ());
      this.resetFallDistance();
      this.setSharedFlag(7, false);
    } else {
      super.travel(travelVector);
    }
  }

  public void sendPositionChanges() {
    // Detect whether anything changed
    var xDiff = x - lastX;
    var yDiff = y - lastY;
    var zDiff = z - lastZ;
    var yawDiff = (double) (yaw - lastYaw);
    var pitchDiff = (double) (pitch - lastPitch);
    var sendPos =
      MathHelper.lengthSquared(xDiff, yDiff, zDiff) > MathHelper.square(2.0E-4)
        || ++positionReminder >= 20;
    var sendRot = pitchDiff != 0.0 || yawDiff != 0.0;
    var sendOnGround = onGround != lastOnGround;

    // Send position packets if changed
    if (sendPos && sendRot) {
      sendPosRot();
    } else if (sendPos) {
      sendPos();
    } else if (sendRot) {
      sendRot();
    } else if (sendOnGround) {
      sendOnGround();
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
    var onGround = movementState.onGround;

    lastOnGround = onGround;

    lastX = x;
    lastY = y;
    lastZ = z;
    positionReminder = 0;

    lastYaw = yaw;
    lastPitch = pitch;

    connection.sendPacket(
      new ServerboundMovePlayerPosRotPacket(onGround, x, y, z, yaw, pitch));
  }

  public void sendPos() {
    var onGround = movementState.onGround;

    lastOnGround = onGround;

    lastX = x;
    lastY = y;
    lastZ = z;
    positionReminder = 0;

    connection.sendPacket(new ServerboundMovePlayerPosPacket(onGround, x, y, z));
  }

  public void sendRot() {
    var onGround = movementState.onGround;

    lastOnGround = onGround;

    lastYaw = yaw;
    lastPitch = pitch;

    connection.sendPacket(new ServerboundMovePlayerRotPacket(onGround, yaw, pitch));
  }

  public void sendOnGround() {
    var onGround = movementState.onGround;

    lastOnGround = onGround;

    connection.sendPacket(new ServerboundMovePlayerStatusOnlyPacket(onGround));
  }

  @Override
  public boolean isSpectator() {
    return dataManager.gameMode() == GameMode.SPECTATOR;
  }

  @Override
  public float getSpeed() {
    return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
  }

  @Override
  public boolean isImmobile() {
    return super.isImmobile() || this.isSleeping();
  }

  public boolean isSleeping() {
    return false; // TODO: Implement
  }
}
