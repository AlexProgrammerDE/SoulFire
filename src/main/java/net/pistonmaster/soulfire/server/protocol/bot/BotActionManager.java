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

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.soulfire.server.data.AttributeType;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.EntityType;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.protocol.bot.movement.AABB;
import net.pistonmaster.soulfire.server.protocol.bot.state.EntityTrackerState;
import net.pistonmaster.soulfire.server.protocol.bot.state.LevelState;
import net.pistonmaster.soulfire.server.protocol.bot.state.PlayerListState;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.Entity;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.RawEntity;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.util.Segment;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;

/**
 * Manages mostly block and interaction related stuff that requires to keep track of sequence numbers.
 */
@Data
@RequiredArgsConstructor
public class BotActionManager {
    private static final float EYE_HEIGHT = 1.62f;
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private int sequenceNumber = 0;
    private long lastHit = 0;
    private Map<String, Object> extraData = new HashMap<>();

    private static Optional<Vector3f> rayCastToBlock(BlockState blockState, Vector3d eyePosition, Vector3d headRotation, Vector3i targetBlock) {
        var intersections = new ArrayList<Vector3f>();

        for (var shape : blockState.getCollisionBoxes(targetBlock)) {
            shape.getIntersection(eyePosition, headRotation)
                    .map(Vector3d::toFloat)
                    .ifPresent(intersections::add);
        }

        if (intersections.isEmpty()) {
            return Optional.empty();
        }

        Vector3f closestIntersection = null;
        var closestDistance = Double.MAX_VALUE;

        for (var intersection : intersections) {
            double distance = intersection.distance(eyePosition.getX(), eyePosition.getY(), eyePosition.getZ());

            if (distance < closestDistance) {
                closestIntersection = intersection;
                closestDistance = distance;
            }
        }

        assert closestIntersection != null;
        return Optional.of(closestIntersection);
    }

    public static Vector3d getMiddleBlockFace(Vector3i blockPos, Direction blockFace) {
        var blockPosDouble = blockPos.toDouble();
        return switch (blockFace) {
            case DOWN -> blockPosDouble.add(0.5, 0, 0.5);
            case UP -> blockPosDouble.add(0.5, 1, 0.5);
            case NORTH -> blockPosDouble.add(0.5, 0.5, 0);
            case SOUTH -> blockPosDouble.add(0.5, 0.5, 1);
            case WEST -> blockPosDouble.add(0, 0.5, 0.5);
            case EAST -> blockPosDouble.add(1, 0.5, 0.5);
        };
    }

    public void incrementSequenceNumber() {
        sequenceNumber++;
    }

    public void useItemInHand(Hand hand) {
        incrementSequenceNumber();
        dataManager.sendPacket(new ServerboundUseItemPacket(hand, sequenceNumber));
    }

    public void placeBlock(Hand hand, BlockPlaceData blockPlaceData) {
        placeBlock(hand, blockPlaceData.againstPos().toVector3i(), blockPlaceData.blockFace());
    }

    public void placeBlock(Hand hand, Vector3i againstBlock, Direction againstFace) {
        incrementSequenceNumber();
        var clientEntity = dataManager.clientEntity();
        var levelState = dataManager.getCurrentLevel();
        if (levelState == null) {
            return;
        }

        var eyePosition = clientEntity.getEyePosition();
        var insideBlock = !levelState.getCollisionBoxes(new AABB(eyePosition, eyePosition)).isEmpty();

        var againstPlacePosition = getMiddleBlockFace(againstBlock, againstFace);

        var previousYaw = clientEntity.yaw();
        var previousPitch = clientEntity.pitch();
        clientEntity.lookAt(RotationOrigin.EYES, againstPlacePosition);
        if (previousPitch != clientEntity.pitch() || previousYaw != clientEntity.yaw()) {
            clientEntity.sendRot();
        }

        var rayCast = rayCastToBlock(levelState.getBlockStateAt(againstBlock), eyePosition, clientEntity.getRotationVector(), againstBlock);
        if (rayCast.isEmpty()) {
            return;
        }

        var rayCastPosition = rayCast.get().sub(againstBlock.toFloat());

        dataManager.sendPacket(new ServerboundUseItemOnPacket(
                againstBlock,
                againstFace,
                hand,
                rayCastPosition.getX(),
                rayCastPosition.getY(),
                rayCastPosition.getZ(),
                insideBlock,
                sequenceNumber
        ));
    }

    public void sendStartBreakBlock(Vector3i blockPos) {
        incrementSequenceNumber();
        var blockFace = getBlockFaceLookedAt(blockPos);
        dataManager.sendPacket(new ServerboundPlayerActionPacket(
                PlayerAction.START_DIGGING,
                blockPos,
                blockFace,
                sequenceNumber
        ));
    }

    public void sendEndBreakBlock(Vector3i blockPos) {
        incrementSequenceNumber();
        var blockFace = getBlockFaceLookedAt(blockPos);
        dataManager.sendPacket(new ServerboundPlayerActionPacket(
                PlayerAction.FINISH_DIGGING,
                blockPos,
                blockFace,
                sequenceNumber
        ));
    }

    public Direction getBlockFaceLookedAt(Vector3i blockPos) {
        var clientEntity = dataManager.clientEntity();
        var eyePosition = clientEntity.getEyePosition();
        var headRotation = clientEntity.getRotationVector();
        var blockPosDouble = blockPos.toDouble();
        var blockBoundingBox = new AABB(blockPosDouble, blockPosDouble.add(1, 1, 1));
        var intersection = blockBoundingBox.getIntersection(eyePosition, headRotation).map(Vector3d::toFloat);
        if (intersection.isEmpty()) {
            return null;
        }

        var intersectionFloat = intersection.get();
        var blockPosFloat = blockPos.toFloat();
        var relativeIntersection = intersectionFloat.sub(blockPosFloat);

        // Check side the intersection is the closest to
        if (relativeIntersection.getX() > relativeIntersection.getY() && relativeIntersection.getX() > relativeIntersection.getZ()) {
            return intersectionFloat.getX() > blockPosFloat.getX() ? Direction.EAST : Direction.WEST;
        } else if (relativeIntersection.getY() > relativeIntersection.getZ()) {
            return intersectionFloat.getY() > blockPosFloat.getY() ? Direction.UP : Direction.DOWN;
        } else {
            return intersectionFloat.getZ() > blockPosFloat.getZ() ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public void sendBreakBlockAnimation() {
        dataManager.sendPacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }

    public void lookAt(@NonNull Entity entity) {
        double x = entity.x() - dataManager.clientEntity().x();
        double y = (entity.y() + entity.height() / 2f) // Center of entity
                - (dataManager.clientEntity().y() + EYE_HEIGHT); // Eye height

        final int VER_1_14 = ProtocolVersion.v1_14.getVersion();
        ProtocolVersion ver = dataManager.settingsHolder().get(BotSettings.PROTOCOL_VERSION, ProtocolVersion::getClosest);
        int version = ver.getVersion();

        if (dataManager.controlState().sneaking()) {
            if (version >= VER_1_14) {
                // the sneak offset is 0.15 lower in 1.14+
                y += 0.15f * 2;
            }
        }

        double z = entity.z() - dataManager.clientEntity().z();

        double distance = Math.sqrt(x * x + y * y + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, distance));

        dataManager.clientEntity().yaw(yaw);
        dataManager.clientEntity().pitch(pitch);
    }

    public void lookAt(@NonNull Vector3d vec) {
        double x = vec.getX() - dataManager.clientEntity().x();
        double y = vec.getY() - (dataManager.clientEntity().y() + EYE_HEIGHT); // Eye height
        double z = vec.getZ() - dataManager.clientEntity().z();

        double distance = Math.sqrt(x * x + y * y + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, distance));

        dataManager.clientEntity().yaw(yaw);
        dataManager.clientEntity().pitch(pitch);
    }

    public Vector3d getEntityVisiblePoint(Entity entity) {
        List<Vector3d> points = new ArrayList<>();
        double halfWidth = entity.width() / 2;
        double halfHeight = entity.height() / 2;
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    points.add(Vector3d.from(entity.x() + halfWidth * x, entity.y() + halfHeight * y, entity.z() + halfWidth * z));
                }
            }
        }

        // sort by distance to the bot
        points.sort(Comparator.comparingDouble(this::distanceTo));
        for (Vector3d point : points) {
            if (canSee(point)) {
                return point;
            }
        }

        return null;
    }

    public void attack(@NonNull Entity entity, boolean swingArm) {
        if (!entity.canBeInterracted()) {
            System.err.println("Entity " + entity.entityId() + " can't be interacted with!");
            return;
        }

        if (swingArm) {
            swingArm();
        }
        ServerboundInteractPacket packet = new ServerboundInteractPacket(entity.entityId(), InteractAction.ATTACK, dataManager.controlState().sneaking());
        dataManager.sendPacket(packet);
        lastHit = System.currentTimeMillis();
    }

    public Entity getClosestEntity(double range, String whitelistedUser, boolean ignoreBots, boolean onlyInterractable, boolean mustBeSeen) {
        if (dataManager.clientEntity() == null) {
            return null;
        }

        double x = dataManager.clientEntity().x();
        double y = dataManager.clientEntity().y();
        double z = dataManager.clientEntity().z();

        EntityTrackerState ets = dataManager.entityTrackerState();
        Map<Integer, Entity> entities = ets.entities();

        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : entities.values()) {
            if (entity.entityId() == dataManager.clientEntity().entityId()) {
                continue;
            }

            if (onlyInterractable && !entity.canBeInterracted()) continue;
            if (mustBeSeen && !canSee(entity)) continue;


            if (whitelistedUser != null && !whitelistedUser.isEmpty() && entity.entityType() == EntityType.PLAYER) {
                PlayerListState connectedUsers = dataManager.playerListState();
                PlayerListEntry playerListEntry = connectedUsers.entries().get(((RawEntity) entity).uuid());
                if (playerListEntry.getProfile() != null) {
                    if (playerListEntry.getProfile().getName().equalsIgnoreCase(whitelistedUser))
                        continue;
                }
            }


            if (ignoreBots && entity instanceof RawEntity rawEntity) {
                Set<Integer> botIds = new HashSet<>();
                dataManager.connection().attackManager().botConnections().forEach(b -> {
                    if (b.sessionDataManager() != null && b.sessionDataManager().clientEntity() != null) {
                        botIds.add(b.sessionDataManager().clientEntity().entityId());
                    }
                });

                if (botIds.contains(rawEntity.entityId())) continue;
            }

            double distance = Math.sqrt(Math.pow(entity.x() - x, 2) + Math.pow(entity.y() - y, 2) + Math.pow(entity.z() - z, 2));
            if (distance > range) continue;

            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public double distanceTo(Entity entity) {
        double middleHeight = entity.y() + entity.height() / 2f;
        Vector3d vec = Vector3d.from(entity.x(), middleHeight, entity.z());
        return distanceTo(vec);
    }

    public double distanceTo(Vector3d vec) {
        if (dataManager.clientEntity() == null) {
            return -1;
        }

        double x = vec.getX() - dataManager.clientEntity().x();
        double y = vec.getY() - (dataManager.clientEntity().y() + 1.80f); // Eye height
        double z = vec.getZ() - dataManager.clientEntity().z();

        return Math.sqrt(x * x + y * y + z * z);
    }

    public boolean canSee(Entity entity) {
        return getEntityVisiblePoint(entity) != null;
    }

    public boolean canSee(Vector3d vec) {
        double distance = distanceTo(vec);
        if (distance >= 256) {
            return false;
        }
        Vector3d eye = dataManager.clientEntity().getEyePosition();
        Segment segment = new Segment(eye, vec);
        for (Map.Entry<String, LevelState> entry : dataManager.levels().entrySet()) {
            List<AABB> boxes = entry.getValue().getCollisionBoxes(new AABB(eye, vec));
            if (segment.intersects(boxes)) {
                return false;
            }
        }

        return true;
    }

    public void swingArm() {
        ServerboundSwingPacket swingPacket = new ServerboundSwingPacket(Hand.MAIN_HAND);
        dataManager.sendPacket(swingPacket);
    }

    public long getCooldownRemainingTime() {
        if (dataManager == null || dataManager.inventoryManager() == null || dataManager.inventoryManager().getPlayerInventory() == null) {
            return 2000;
        }

        int itemSlot = dataManager.inventoryManager().heldItemSlot();
        SWItemStack item = dataManager.inventoryManager().getPlayerInventory().hotbarSlot(itemSlot).item();
        int cooldown = 500; // Default cooldown when you hit with your hand
        if (item != null) {
            cooldown = dataManager.itemCoolDowns().get(item.type().id()) * 50; // 50ms per tick
            if (cooldown == 0) { // if the server hasn't changed the cooldown
                double attackSpeedModifier = item.type().attributes().stream()
                        .filter(attribute -> attribute.type() == AttributeType.GENERIC_ATTACK_SPEED)
                        .map(attribute -> attribute.modifiers().get(0).amount())
                        .findFirst()
                        .orElse(0d);  // Default attack speed

                double attackSpeed = 4.0 + attackSpeedModifier;
                cooldown = (int) ((1 / attackSpeed) * 1000);

            }
        }
        return lastHit + cooldown - System.currentTimeMillis();
    }

    public record BlockPlaceData(SWVec3i againstPos, Direction blockFace) {
    }
}
