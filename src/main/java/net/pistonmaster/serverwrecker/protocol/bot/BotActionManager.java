/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.serverwrecker.data.BlockShapeType;
import net.pistonmaster.serverwrecker.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages mostly block and interaction related stuff that requires to keep track of sequence numbers.
 */
@Data
@RequiredArgsConstructor
public class BotActionManager {
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private int sequenceNumber = 0;

    public static Optional<BlockPlaceData> findBlockToPlaceAgainst(Map<Vector3i, Optional<BlockStateMeta>> blockCache,
                                                                   ProjectedLevelState levelState, Vector3i targetPos,
                                                                   List<Vector3i> ignoreBlocks) {
        for (var direction : Direction.values()) {
            var blockPos = targetPos.add(switch (direction) {
                case DOWN -> Vector3i.from(0, 1, 0);
                case UP -> Vector3i.from(0, -1, 0);
                case NORTH -> Vector3i.from(0, 0, 1);
                case SOUTH -> Vector3i.from(0, 0, -1);
                case WEST -> Vector3i.from(1, 0, 0);
                case EAST -> Vector3i.from(-1, 0, 0);
            });

            if (ignoreBlocks.contains(blockPos)) {
                continue;
            }

            var blockState = levelState.getCachedBlockStateAt(blockCache, blockPos);
            if (blockState.isEmpty() || !blockState.get().blockShapeType().isFullBlock()) {
                continue;
            }

            return Optional.of(new BlockPlaceData(blockPos, direction));
        }

        return Optional.empty();
    }

    public void incrementSequenceNumber() {
        sequenceNumber++;
    }

    public void useItemInHand(Hand hand) {
        incrementSequenceNumber();
        dataManager.getSession().send(new ServerboundUseItemPacket(hand, sequenceNumber));
    }

    public void placeBlock(Hand hand, BlockPlaceData blockPlaceData) {
        placeBlock(hand, blockPlaceData.againstPos(), blockPlaceData.blockFace());
    }

    public void placeBlock(Hand hand, Vector3i againstBlock, Direction againstFace) {
        incrementSequenceNumber();
        var movementManager = dataManager.getBotMovementManager();
        var levelState = dataManager.getCurrentLevel();
        if (levelState == null) {
            return;
        }

        var eyePosition = movementManager.getEyePosition();
        var insideBlock = !levelState.getCollisionBoxes(new BoundingBox(eyePosition, eyePosition)).isEmpty();

        var againstPlacePosition = getMiddleBlockFace(againstBlock, againstFace);
        var previousYaw = movementManager.getYaw();
        var previousPitch = movementManager.getPitch();
        movementManager.lookAt(RotationOrigin.EYES, againstPlacePosition);
        if (previousPitch != movementManager.getPitch() || previousYaw != movementManager.getYaw()) {
            movementManager.sendRot();
        }

        var blockState = levelState.getBlockStateAt(againstBlock);
        if (blockState.isEmpty()) {
            return;
        }

        var rayCast = rayCastToBlock(blockState.get().blockShapeType(), eyePosition, movementManager.getRotationVector(), againstBlock);
        if (rayCast.isEmpty()) {
            return;
        }

        var rayCastPosition = rayCast.get().sub(againstBlock.toFloat());

        dataManager.getSession().send(new ServerboundUseItemOnPacket(
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

    private Optional<Vector3f> rayCastToBlock(BlockShapeType shapeType, Vector3d eyePosition, Vector3d headRotation, Vector3i targetBlock) {
        var intersections = new ArrayList<Vector3f>();

        for (var shape : shapeType.blockShapes()) {
            var boundingBox = shape.createBoundingBoxAt(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
            boundingBox.getIntersection(eyePosition, headRotation)
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

    public Vector3d getMiddleBlockFace(Vector3i blockPos, Direction blockFace) {
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

    public void sendStartBreakBlock(Vector3i blockPos) {
        incrementSequenceNumber();
        var blockFace = getBlockFaceLookedAt(blockPos);
        dataManager.getSession().send(new ServerboundPlayerActionPacket(
                PlayerAction.START_DIGGING,
                blockPos,
                blockFace,
                sequenceNumber
        ));
    }

    public void sendEndBreakBlock(Vector3i blockPos) {
        incrementSequenceNumber();
        var blockFace = getBlockFaceLookedAt(blockPos);
        dataManager.getSession().send(new ServerboundPlayerActionPacket(
                PlayerAction.FINISH_DIGGING,
                blockPos,
                blockFace,
                sequenceNumber
        ));
    }

    public Direction getBlockFaceLookedAt(Vector3i blockPos) {
        var eyePosition = dataManager.getBotMovementManager().getEyePosition();
        var headRotation = dataManager.getBotMovementManager().getRotationVector();
        var blockPosDouble = blockPos.toDouble();
        var blockBoundingBox = new BoundingBox(blockPosDouble, blockPosDouble.add(1, 1, 1));
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

    public record BlockPlaceData(Vector3i againstPos, Direction blockFace) {
    }
}
