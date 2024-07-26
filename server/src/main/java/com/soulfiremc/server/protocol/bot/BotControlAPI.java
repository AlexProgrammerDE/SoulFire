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
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.bot.movement.AABB;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.util.Segment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

/**
 * This class is used to control the bot. The goal is to reduce friction for doing simple things.
 */
@Slf4j
@RequiredArgsConstructor
public class BotControlAPI {
  private final BotConnection connection;
  private final SessionDataManager dataManager;
  private final SecureRandom secureRandom = new SecureRandom();
  @Getter
  @Setter
  private int attackCooldownTicks = 0;

  public void tick() {
    if (attackCooldownTicks > 0) {
      attackCooldownTicks--;
    }
  }

  public boolean toggleFlight() {
    var abilitiesData = dataManager.abilitiesData();
    if (abilitiesData != null && !abilitiesData.allowFlying()) {
      throw new IllegalStateException("You can't fly! (Server said so)");
    }

    var newFly = !dataManager.controlState().flying();
    dataManager.controlState().flying(newFly);

    // Let the server know we are flying
    connection.sendPacket(new ServerboundPlayerAbilitiesPacket(newFly));

    return newFly;
  }

  public boolean toggleSprint() {
    var newSprint = !dataManager.controlState().sprinting();
    dataManager.controlState().sprinting(newSprint);

    // Let the server know we are sprinting
    connection.sendPacket(
      new ServerboundPlayerCommandPacket(
        dataManager.clientEntity().entityId(),
        newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING));

    return newSprint;
  }

  public boolean toggleSneak() {
    var newSneak = !dataManager.controlState().sneaking();
    dataManager.controlState().sneaking(newSneak);

    // Let the server know we are sneaking
    connection.sendPacket(
      new ServerboundPlayerCommandPacket(
        dataManager.clientEntity().entityId(),
        newSneak ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING));

    return newSneak;
  }

  public void sendMessage(String message) {
    var now = Instant.now();
    if (message.startsWith("/")) {
      var command = message.substring(1);
      // We only sign chat at the moment because commands require the entire command tree to be
      // handled
      // Command signing is signing every string parameter in the command because of reporting /msg
      connection.sendPacket(
        new ServerboundChatCommandPacket(command));
    } else {
      var salt = secureRandom.nextLong();
      connection.sendPacket(
        new ServerboundChatPacket(message, now.toEpochMilli(), salt, null, 0, new BitSet()));
    }
  }

  public void registerPluginChannels(Key... channels) {
    var buffer = Unpooled.buffer();
    for (var i = 0; i < channels.length; i++) {
      var channel = channels[i];
      buffer.writeBytes(channel.toString().getBytes(StandardCharsets.UTF_8));

      if (i != channels.length - 1) {
        buffer.writeByte(0);
      }
    }

    sendPluginMessage(SFProtocolConstants.REGISTER_KEY, buffer);
  }

  public void sendPluginMessage(Key channel, ByteBuf data) {
    var array = new byte[data.readableBytes()];
    data.readBytes(array);

    sendPluginMessage(channel, array);
  }

  public void sendPluginMessage(Key channel, byte[] data) {
    connection.sendPacket(new ServerboundCustomPayloadPacket(channel, data));
  }

  public Vector3d getEntityVisiblePoint(Entity entity) {
    var points = new ArrayList<Vector3d>();
    double halfWidth = entity.width() / 2;
    double halfHeight = entity.height() / 2;
    for (var x = -1; x <= 1; x++) {
      for (var y = 0; y <= 2; y++) {
        for (var z = -1; z <= 1; z++) {
          // skip the middle point because you're supposed to look at hitbox faces
          if (x == 0 && y == 1 && z == 0) {
            continue;
          }
          points.add(
            Vector3d.from(
              entity.x() + halfWidth * x,
              entity.y() + halfHeight * y,
              entity.z() + halfWidth * z));
        }
      }
    }

    var eye = dataManager.clientEntity().eyePosition();

    // sort by distance to the bot
    points.sort(Comparator.comparingDouble(eye::distance));

    // remove the farthest points because they're not "visible"
    for (var i = 0; i < 4; i++) {
      points.removeLast();
    }

    for (var point : points) {
      if (canSee(point)) {
        return point;
      }
    }

    return null;
  }

  public void attack(@NonNull Entity entity, boolean swingArm) {
    if (!entity.entityType().attackable()) {
      log.error("Entity {} can't be attacked!", entity.entityId());
      return;
    }

    var packet =
      new ServerboundInteractPacket(
        entity.entityId(), InteractAction.ATTACK, dataManager.controlState().sneaking());
    connection.sendPacket(packet);
    if (swingArm) {
      swingArm();
    }

    attackCooldownTicks = (int) getHitItemCooldownTicks();
  }

  public Entity getClosestEntity(
    double range,
    String whitelistedUser,
    boolean ignoreBots,
    boolean onlyInteractable,
    boolean mustBeSeen) {
    if (dataManager.clientEntity() == null) {
      return null;
    }

    var x = dataManager.clientEntity().x();
    var y = dataManager.clientEntity().y();
    var z = dataManager.clientEntity().z();

    Entity closest = null;
    var closestDistance = Double.MAX_VALUE;

    for (var entity : dataManager.entityTrackerState().getEntities()) {
      if (entity.entityId() == dataManager.clientEntity().entityId()) {
        continue;
      }

      var distance =
        Math.sqrt(
          Math.pow(entity.x() - x, 2)
            + Math.pow(entity.y() - y, 2)
            + Math.pow(entity.z() - z, 2));
      if (distance > range) {
        continue;
      }

      if (onlyInteractable && !entity.entityType().attackable()) {
        continue;
      }

      if (whitelistedUser != null
        && !whitelistedUser.isEmpty()
        && entity.entityType() == EntityType.PLAYER) {
        var connectedUsers = dataManager.playerListState();
        var playerListEntry = connectedUsers.entries().get(entity.uuid());
        if (playerListEntry != null && playerListEntry.getProfile() != null) {
          if (playerListEntry.getProfile().getName().equalsIgnoreCase(whitelistedUser)) {
            continue;
          }
        }
      }

      if (ignoreBots
        && dataManager.connection().instanceManager().botConnections().values().stream()
        .anyMatch(
          b -> {
            if (b.dataManager().clientEntity() == null) {
              return false;
            }

            return b.dataManager().clientEntity().uuid().equals(entity.uuid());
          })) {
        continue;
      }

      if (mustBeSeen && !canSee(entity)) {
        continue;
      }

      if (distance < closestDistance) {
        closest = entity;
        closestDistance = distance;
      }
    }

    return closest;
  }

  public boolean canSee(Entity entity) {
    return getEntityVisiblePoint(entity) != null;
  }

  public boolean canSee(Vector3d vec) { // intensive method, don't use it too often
    var level = dataManager.currentLevel();

    var eye = dataManager.clientEntity().eyePosition();
    var distance = eye.distance(vec);
    if (distance >= 256) {
      return false;
    }

    if (!level.isChunkLoaded(Vector3i.from(vec.getX(), vec.getY(), vec.getZ()))) {
      return false;
    }

    var segment = new Segment(eye, vec);
    var boxes = level.getCollisionBoxes(new AABB(eye, vec));
    return !segment.intersects(boxes);
  }

  public void swingArm() {
    var swingPacket = new ServerboundSwingPacket(Hand.MAIN_HAND);
    connection.sendPacket(swingPacket);
  }

  public float getHitItemCooldownTicks() {
    dataManager.inventoryManager().applyItemAttributes();

    return (float)
      (1.0
        / dataManager.clientEntity().attributeValue(AttributeType.ATTACK_SPEED)
        * 20.0);
  }
}
