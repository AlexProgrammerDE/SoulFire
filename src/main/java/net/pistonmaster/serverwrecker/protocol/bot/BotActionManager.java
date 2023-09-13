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
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.serverwrecker.data.BlockShape;
import net.pistonmaster.serverwrecker.data.BlockShapeType;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;
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

    public void incrementSequenceNumber() {
        sequenceNumber++;
    }

    public void useItemInHand(Hand hand) {
        incrementSequenceNumber();
        dataManager.getSession().send(new ServerboundUseItemPacket(hand, sequenceNumber));
    }

    public void placeBlock(Hand hand, Vector3i againstBlock, Direction againstFace) {
        incrementSequenceNumber();
        BotMovementManager movementManager = dataManager.getBotMovementManager();
        LevelState levelState = dataManager.getCurrentLevel();
        if (levelState == null) {
            return;
        }

        Vector3d eyePosition = movementManager.getEyePosition();
        boolean insideBlock = !levelState.getCollisionBoxes(new BoundingBox(eyePosition, eyePosition)).isEmpty();

        Vector3d againstPlacePosition = getMiddleBlockFace(againstBlock, againstFace);
        float previousYaw = movementManager.getYaw();
        float previousPitch = movementManager.getPitch();
        movementManager.lookAt(RotationOrigin.EYES, againstPlacePosition);
        if (previousPitch != movementManager.getPitch() || previousYaw != movementManager.getYaw()) {
            movementManager.sendRot();
        }

        Optional<BlockStateMeta> blockState = levelState.getBlockStateAt(againstBlock);
        if (blockState.isEmpty()) {
            return;
        }

        Optional<Vector3f> rayCast = rayCastToBlock(blockState.get().blockShapeType(), eyePosition, movementManager.getRotationVector(), againstPlacePosition);
        if (rayCast.isEmpty()) {
            return;
        }

        Vector3f rayCastPosition = rayCast.get().min(againstBlock.toFloat());

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

    private Optional<Vector3f> rayCastToBlock(BlockShapeType shapeType, Vector3d eyePosition, Vector3d headRotation, Vector3d targetPosition) {
        List<Vector3f> intersections = new ArrayList<>();

        for (BlockShape shape : shapeType.blockShapes()) {
            BoundingBox boundingBox = new BoundingBox(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ());
            boundingBox.move(targetPosition.getX(), targetPosition.getY(), targetPosition.getZ());
            boundingBox.getIntersection(eyePosition, headRotation).map(Vector3d::toFloat).ifPresent(intersections::add);
        }

        if (intersections.isEmpty()) {
            return Optional.empty();
        }

        Vector3f closestIntersection = null;
        double closestDistance = Double.MAX_VALUE;

        for (Vector3f intersection : intersections) {
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
        Vector3d blockPosDouble = blockPos.toDouble();
        return switch (blockFace) {
            case DOWN -> blockPosDouble.add(0.5, 0, 0.5);
            case UP -> blockPosDouble.add(0.5, 1, 0.5);
            case NORTH -> blockPosDouble.add(0.5, 0.5, 0);
            case SOUTH -> blockPosDouble.add(0.5, 0.5, 1);
            case WEST -> blockPosDouble.add(0, 0.5, 0.5);
            case EAST -> blockPosDouble.add(1, 0.5, 0.5);
        };
    }

    public Optional<BlockPlaceData> findBlockToPlaceAgainst(Vector3i targetPos, List<Vector3i> ignoreBlocks) {
        LevelState levelState = dataManager.getCurrentLevel();
        if (levelState == null) {
            return Optional.empty();
        }

        for (Direction direction : Direction.values()) {
            Vector3i blockPos = targetPos.add(switch (direction) {
                case DOWN -> Vector3i.from(0, -1, 0);
                case UP -> Vector3i.from(0, 1, 0);
                case NORTH -> Vector3i.from(0, 0, -1);
                case SOUTH -> Vector3i.from(0, 0, 1);
                case WEST -> Vector3i.from(-1, 0, 0);
                case EAST -> Vector3i.from(1, 0, 0);
            });

            if (ignoreBlocks.contains(blockPos)) {
                continue;
            }

            Optional<BlockStateMeta> blockState = levelState.getBlockStateAt(blockPos);
            if (blockState.isEmpty() || !blockState.get().blockShapeType().isFullBlock()) {
                continue;
            }

            return Optional.of(new BlockPlaceData(blockPos, direction));
        }

        return Optional.empty();
    }

    public record BlockPlaceData(Vector3i againstPos, Direction blockFace) {
    }
}
