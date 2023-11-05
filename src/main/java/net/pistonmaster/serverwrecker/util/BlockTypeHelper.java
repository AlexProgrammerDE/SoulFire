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
package net.pistonmaster.serverwrecker.util;

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.data.ResourceData;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;

public class BlockTypeHelper {
    private BlockTypeHelper() {
    }

    public static boolean isReplaceable(BlockType type) {
        return ResourceData.BLOCK_PROPERTY_MAP.get(type.id()).replaceable();
    }

    public static boolean isAir(BlockType type) {
        return type == BlockType.AIR
                || type == BlockType.CAVE_AIR
                || type == BlockType.VOID_AIR;
    }

    public static boolean isFluid(BlockType type) {
        return type == BlockType.WATER
                || type == BlockType.LAVA
                || type == BlockType.BUBBLE_COLUMN;
    }

    public static boolean isEmpty(BlockStateMeta meta) {
        return meta.blockShapeType().hasNoCollisions();
    }

    public static boolean isFullBlock(BlockStateMeta meta) {
        return meta.blockShapeType().isFullBlock();
    }

    public static boolean isHurtOnTouchSide(BlockType type) {
        return type == BlockType.CACTUS
                || type == BlockType.SWEET_BERRY_BUSH
                || type == BlockType.WITHER_ROSE
                || type == BlockType.FIRE
                || type == BlockType.SOUL_FIRE
                || type == BlockType.LAVA;
    }

    public static boolean isFallingAroundMinedBlock(BlockType type) {
        return type == BlockType.SAND
                || type == BlockType.GRAVEL
                || type == BlockType.ANVIL
                || type == BlockType.CHIPPED_ANVIL
                || type == BlockType.DAMAGED_ANVIL;
    }

    public static boolean isHurtWhenStoodOn(BlockType type) {
        return type == BlockType.MAGMA_BLOCK;
    }

    public static boolean isSafeBlockToStandOn(BlockStateMeta meta) {
        return meta.blockShapeType().isFullBlock() && !isHurtWhenStoodOn(meta.blockType());
    }
}
