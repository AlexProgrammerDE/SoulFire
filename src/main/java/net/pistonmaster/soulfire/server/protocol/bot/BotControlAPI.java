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
package net.pistonmaster.soulfire.server.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.server.data.EntityType;
import net.pistonmaster.soulfire.server.protocol.bot.movement.AABB;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.Entity;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.RawEntity;
import net.pistonmaster.soulfire.server.util.Segment;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * This class is used to control the bot.
 * The goal is to reduce friction for doing simple things.
 */
@RequiredArgsConstructor
public class BotControlAPI {
    private final SessionDataManager dataManager;
    private final SecureRandom secureRandom = new SecureRandom();
    @Getter
    private final Map<String, Object> extraData = new HashMap<>();
    @Getter
    private long lastHit = 0;

    public boolean toggleFlight() {
        var abilitiesData = dataManager.abilitiesData();
        if (abilitiesData != null && !abilitiesData.allowFlying()) {
            throw new IllegalStateException("You can't fly! (Server said so)");
        }

        var newFly = !dataManager.controlState().flying();
        dataManager.controlState().flying(newFly);

        // Let the server know we are flying
        dataManager.sendPacket(new ServerboundPlayerAbilitiesPacket(newFly));

        return newFly;
    }

    public boolean toggleSprint() {
        var newSprint = !dataManager.controlState().sprinting();
        dataManager.controlState().sprinting(newSprint);

        // Let the server know we are sprinting
        dataManager.sendPacket(new ServerboundPlayerCommandPacket(
                dataManager.clientEntity().entityId(),
                newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING
        ));

        return newSprint;
    }

    public boolean toggleSneak() {
        var newSneak = !dataManager.controlState().sneaking();
        dataManager.controlState().sneaking(newSneak);

        // Let the server know we are sneaking
        dataManager.sendPacket(new ServerboundPlayerCommandPacket(
                dataManager.clientEntity().entityId(),
                newSneak ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING
        ));

        return newSneak;
    }

    public void sendMessage(String message) {
        var now = Instant.now();
        if (message.startsWith("/")) {
            var command = message.substring(1);
            // We only sign chat at the moment because commands require the entire command tree to be handled
            // Command signing is signing every string parameter in the command because of reporting /msg
            dataManager.sendPacket(new ServerboundChatCommandPacket(
                    command,
                    now.toEpochMilli(),
                    0L,
                    Collections.emptyList(),
                    0,
                    new BitSet()
            ));
        } else {
            var salt = secureRandom.nextLong();
            dataManager.sendPacket(new ServerboundChatPacket(
                    message,
                    now.toEpochMilli(),
                    salt,
                    null,
                    0,
                    new BitSet()
            ));
        }
    }

    public void registerPluginChannels(String... channels) {
        var buffer = Unpooled.buffer();
        for (var i = 0; i < channels.length; i++) {
            var channel = channels[i];
            buffer.writeBytes(channel.getBytes(StandardCharsets.UTF_8));

            if (i != channels.length - 1) {
                buffer.writeByte(0);
            }
        }

        sendPluginMessage("minecraft:register", buffer);
    }

    public void sendPluginMessage(String channel, ByteBuf data) {
        var array = new byte[data.readableBytes()];
        data.readBytes(array);

        sendPluginMessage(channel, array);
    }

    public void sendPluginMessage(String channel, byte[] data) {
        dataManager.sendPacket(new ServerboundCustomPayloadPacket(
                channel,
                data
        ));
    }

    public Vector3d getEntityVisiblePoint(Entity entity) {
        var points = new ArrayList<Vector3d>();
        double halfWidth = entity.width() / 2;
        double halfHeight = entity.height() / 2;
        for (var x = -1; x <= 1; x++) {
            for (var y = 0; y <= 2; y++) {
                for (var z = -1; z <= 1; z++) {
                    // skip the middle point because you're supposed to look at hitbox faces
                    if (x == 0 && y == 1 && z == 0) continue;
                    points.add(Vector3d.from(
                            entity.x() + halfWidth * x,
                            entity.y() + halfHeight * y,
                            entity.z() + halfWidth * z
                    ));
                }
            }
        }

        var eye = dataManager.clientEntity().getEyePosition();

        // sort by distance to the bot
        points.sort(Comparator.comparingDouble(eye::distance));

        // remove the farthest points because they're not "visible"
        for (var i = 0; i < 4; i++)
            points.removeLast();

        for (var point : points) {
            if (canSee(point)) {
                return point;
            }
        }

        return null;
    }

    public void attack(@NonNull Entity entity, boolean swingArm) {
        if (!entity.entityType().attackable()) {
            System.err.println("Entity " + entity.entityId() + " can't be attacked!");
            return;
        }

        var packet = new ServerboundInteractPacket(entity.entityId(), InteractAction.ATTACK, dataManager.controlState().sneaking());
        dataManager.sendPacket(packet);
        if (swingArm) {
            swingArm();
        }
        lastHit = System.currentTimeMillis();
    }

    public Entity getClosestEntity(double range, String whitelistedUser, boolean ignoreBots, boolean onlyInteractable, boolean mustBeSeen) {
        if (dataManager.clientEntity() == null) {
            return null;
        }

        var x = dataManager.clientEntity().x();
        var y = dataManager.clientEntity().y();
        var z = dataManager.clientEntity().z();

        Entity closest = null;
        var closestDistance = Double.MAX_VALUE;

        for (var entity : dataManager.entityTrackerState().getEntities()) {
            if (entity.entityId() == dataManager.clientEntity().entityId()) continue;

            var distance = Math.sqrt(Math.pow(entity.x() - x, 2) + Math.pow(entity.y() - y, 2) + Math.pow(entity.z() - z, 2));
            if (distance > range) continue;

            if (onlyInteractable && !entity.entityType().attackable()) continue;

            if (whitelistedUser != null && !whitelistedUser.isEmpty() && entity.entityType() == EntityType.PLAYER) {
                var connectedUsers = dataManager.playerListState();
                var playerListEntry = connectedUsers.entries().get(((RawEntity) entity).uuid());
                if (playerListEntry != null && playerListEntry.getProfile() != null) {
                    if (playerListEntry.getProfile().getName().equalsIgnoreCase(whitelistedUser)) {
                        continue;
                    }
                }
            }

            if (ignoreBots && entity instanceof RawEntity rawEntity
                    && dataManager.connection().attackManager().botConnections().stream().anyMatch(b -> {
                if (b.sessionDataManager() == null || b.sessionDataManager().clientEntity() == null) {
                    return false;
                }

                return b.sessionDataManager().clientEntity().entityId() == rawEntity.entityId();
            })) {
                continue;
            }

            if (mustBeSeen && !canSee(entity)) continue;

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
        var level = dataManager.getCurrentLevel();
        if (level == null) return false;

        var eye = dataManager.clientEntity().getEyePosition();
        var distance = eye.distance(vec);
        if (distance >= 256) return false;

        if (!level.isChunkLoaded(Vector3i.from(vec.getX(), vec.getY(), vec.getZ()))) return false;

        var segment = new Segment(eye, vec);
        var boxes = dataManager.getCurrentLevel().getCollisionBoxes(new AABB(eye, vec));
        return !segment.intersects(boxes);
    }

    public void swingArm() {
        var swingPacket = new ServerboundSwingPacket(Hand.MAIN_HAND);
        dataManager.sendPacket(swingPacket);
    }

    public long getCooldownRemainingTime() {
        if (dataManager == null || dataManager.inventoryManager() == null || dataManager.inventoryManager().getPlayerInventory() == null) {
            return 2000;
        }

        var itemSlot = dataManager.inventoryManager().heldItemSlot();
        var item = dataManager.inventoryManager().getPlayerInventory().hotbarSlot(itemSlot).item();
        var cooldown = 500; // Default cooldown when you hit with your hand
        if (item != null) {
            cooldown = dataManager.itemCoolDowns().get(item.type().id()) * 50; // 50ms per tick
            if (cooldown == 0) { // if the server hasn't changed the cooldown
                var attackSpeedModifier = 0d;
                for (var attribute : item.type().attributes()) {
                    for (var modifier : attribute.modifiers()) {
                        if (modifier.uuid().equals(UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3"))) {
                            attackSpeedModifier = modifier.amount();
                            break;
                        }
                    }
                }

                var attackSpeed = 4.0 + attackSpeedModifier;
                cooldown = (int) ((1 / attackSpeed) * 1000);
            }
        }
        return lastHit + cooldown - System.currentTimeMillis();
    }
}
