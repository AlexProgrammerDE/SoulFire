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
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.movement.BotMovementManager;
import com.soulfiremc.server.protocol.bot.movement.ControlState;
import com.soulfiremc.server.protocol.bot.movement.PhysicsData;
import com.soulfiremc.server.protocol.bot.movement.PlayerMovementState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.util.UUID;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LocalPlayer extends Entity {
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

  public LocalPlayer(
    int entityId, UUID uuid, BotConnection connection, SessionDataManager dataManager,
    Level level) {
    super(entityId, uuid, EntityType.PLAYER, level, 0, 0, 0, -180, 0, -180, 0, 0, 0);
    this.connection = connection;
    this.dataManager = dataManager;
  }

  @Override
  public void tick() {
    if (level.isChunkLoaded(this.blockX(), this.blockZ())) {
      super.tick();

      // Send position changes
      sendPosition();
    }
  }

  private void sendPosition() {
    this.sendIsSprintingIfNeeded();
    boolean bl = this.isShiftKeyDown();
    if (bl != this.wasShiftKeyDown) {
      this.connection.send(new ServerboundPlayerCommandPacket(entityId(), bl
        ? PlayerState.START_SNEAKING
        : PlayerState.STOP_SNEAKING));
      this.wasShiftKeyDown = bl;
    }

    if (this.isControlledCamera()) {
      double d = this.x() - this.xLast;
      double e = this.y() - this.yLast1;
      double f = this.z() - this.zLast;
      double g = (double)(this.getYRot() - this.yRotLast);
      double h = (double)(this.getXRot() - this.xRotLast);
      this.positionReminder++;
      boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
      boolean bl3 = g != 0.0 || h != 0.0;
      if (bl2 && bl3) {
        this.connection
          .send(new ServerboundMovePlayerPosRotPacket(this.x(), this.y(), this.z(), this.getYRot(), this.getXRot(), this.onGround()));
      } else if (bl2) {
        this.connection.send(new ServerboundMovePlayerPosPacket(this.x(), this.y(), this.z(), this.onGround()));
      } else if (bl3) {
        this.connection.send(new ServerboundMovePlayerRotPacket(this.getYRot(), this.getXRot(), this.onGround()));
      } else if (this.lastOnGround != this.onGround()) {
        this.connection.send(new ServerboundMovePlayerStatusOnlyPacket(this.onGround()));
      }

      if (bl2) {
        this.xLast = this.x();
        this.yLast1 = this.y();
        this.zLast = this.z();
        this.positionReminder = 0;
      }

      if (bl3) {
        this.yRotLast = this.getYRot();
        this.xRotLast = this.getXRot();
      }

      this.lastOnGround = this.onGround();
    }
  }

  private void sendIsSprintingIfNeeded() {
    boolean bl = this.isSprinting();
    if (bl != this.wasSprinting) {
      this.connection.send(new ServerboundPlayerCommandPacket(entityId(), bl
        ? PlayerState.START_SPRINTING
        : PlayerState.STOP_SPRINTING));
      this.wasSprinting = bl;
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

  public AbilitiesData abilities() {
    return dataManager.abilitiesData();
  }

  public void jump() {
    movementState.jumpQueued = true;
  }

  @Override
  public float height() {
    return this.controlState.sneaking() ? 1.5F : 1.8F;
  }
}
