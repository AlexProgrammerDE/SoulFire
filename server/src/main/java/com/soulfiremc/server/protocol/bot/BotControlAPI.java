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

import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.bot.state.entity.AbstractClientPlayer;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.protocol.bot.state.entity.LivingEntity;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.structs.Segment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is used to control the bot. The goal is to reduce friction for doing simple things.
 */
@Slf4j
@RequiredArgsConstructor
public class BotControlAPI {
  private final BotConnection connection;
  private final SecureRandom secureRandom = new SecureRandom();
  private final AtomicReference<ControllingTask> controllingTask = new AtomicReference<>();

  public void tick() {
    var localTask = this.controllingTask.get();
    if (localTask != null) {
      if (localTask.isDone()) {
        localTask.stop();
        unregisterControllingTask(localTask);
      } else {
        localTask.tick();

        if (localTask.isDone()) {
          localTask.stop();
          unregisterControllingTask(localTask);
        }
      }
    }
  }

  public boolean stopControllingTask() {
    return this.controllingTask.updateAndGet(
      current -> {
        if (current != null) {
          current.stop();
          return null;
        }

        return null;
      }) != null;
  }

  public boolean activelyControlled() {
    return this.controllingTask.get() != null;
  }

  public void registerControllingTask(ControllingTask task) {
    this.controllingTask.updateAndGet(
      current -> {
        if (current != null) {
          current.stop();
        }

        return task;
      });
  }

  public void unregisterControllingTask(ControllingTask task) {
    this.controllingTask.compareAndSet(task, null);
  }

  public void maybeRegister(ControllingTask task) {
    this.controllingTask.compareAndSet(null, task);
  }

  public <M extends ControllingTask.ManualTaskMarker> M getMarkerAndUnregister(Class<M> clazz) {
    var task = this.controllingTask.get();
    if (task instanceof ControllingTask.ManualControllingTask manual
      && clazz.isInstance(manual.marker())) {
      unregisterControllingTask(task);
      return clazz.cast(manual.marker());
    }

    return null;
  }

  public boolean toggleFlight() {
    var dataManager = connection.dataManager();
    var abilitiesData = dataManager.localPlayer().abilitiesState();
    if (abilitiesData != null && !abilitiesData.mayfly()) {
      throw new IllegalStateException("You can't fly! (Server said so)");
    }

    var newFly = !dataManager.localPlayer().abilitiesState().flying();
    dataManager.localPlayer().abilitiesState().flying(newFly);

    // Let the server know we are flying
    connection.sendPacket(new ServerboundPlayerAbilitiesPacket(newFly));

    return newFly;
  }

  public boolean toggleSprint() {
    var dataManager = connection.dataManager();
    var newSprint = !connection.controlState().sprinting();
    connection.controlState().sprinting(newSprint);

    // Let the server know we are sprinting
    connection.sendPacket(
      new ServerboundPlayerCommandPacket(
        dataManager.localPlayer().entityId(),
        newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING));

    return newSprint;
  }

  public boolean toggleSneak() {
    var dataManager = connection.dataManager();
    var newSneak = !connection.controlState().sneaking();
    connection.controlState().sneaking(newSneak);

    // Let the server know we are sneaking
    connection.sendPacket(
      new ServerboundPlayerCommandPacket(
        dataManager.localPlayer().entityId(),
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
    double halfWidth = entity.dimensions().width() / 2;
    double halfHeight = entity.dimensions().height() / 2;
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

    var dataManager = connection.dataManager();
    var eye = dataManager.localPlayer().eyePosition();

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

  public Entity getClosestEntity(
    double range,
    List<String> whitelistedUsers,
    boolean ignoreBots,
    boolean onlyInteractable,
    boolean mustBeSeen) {
    var dataManager = connection.dataManager();
    if (dataManager.localPlayer() == null) {
      return null;
    }

    var x = dataManager.localPlayer().x();
    var y = dataManager.localPlayer().y();
    var z = dataManager.localPlayer().z();

    Entity closest = null;
    var closestDistance = Double.MAX_VALUE;

    for (var entity : dataManager.currentLevel().getEntities()) {
      if (entity.entityId() == dataManager.localPlayer().entityId()) {
        continue;
      }

      var distance = entity.pos().distance(x, y, z);
      if (distance > range) {
        continue;
      }

      if (onlyInteractable && !entity.entityType().attackable()) {
        continue;
      }

      if (onlyInteractable && entity instanceof LivingEntity le && !le.canBeSeenAsEnemy()) {
        continue;
      }

      if (onlyInteractable && entity instanceof AbstractClientPlayer acp && (acp.isCreative() || acp.isSpectator())) {
        continue;
      }

      if (whitelistedUsers != null
        && !whitelistedUsers.isEmpty()
        && entity.entityType() == EntityType.PLAYER) {
        var connectedUsers = dataManager.playerListState();
        var playerListEntry = connectedUsers.entries().get(entity.uuid());
        if (playerListEntry != null
          && playerListEntry.getProfile() != null
          && whitelistedUsers.stream()
          .anyMatch(whitelistedUser -> playerListEntry.getProfile().getName().equalsIgnoreCase(whitelistedUser))) {
          continue;
        }
      }

      if (ignoreBots
        && dataManager.connection().instanceManager().botConnections().values().stream()
        .anyMatch(
          b -> {
            if (b.dataManager().localPlayer() == null) {
              return false;
            }

            return b.dataManager().localPlayer().uuid().equals(entity.uuid());
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
    var dataManager = connection.dataManager();
    var level = dataManager.currentLevel();

    var eye = dataManager.localPlayer().eyePosition();
    var distance = eye.distance(vec);
    if (distance >= 256) {
      return false;
    }

    var blockVec = vec.toInt();
    if (!level.isChunkPositionLoaded(blockVec.getX(), blockVec.getZ())) {
      return false;
    }

    var segment = new Segment(eye, vec);
    var boxes = level.getBlockCollisionBoxes(new AABB(eye, vec));
    return !segment.intersects(boxes);
  }
}
