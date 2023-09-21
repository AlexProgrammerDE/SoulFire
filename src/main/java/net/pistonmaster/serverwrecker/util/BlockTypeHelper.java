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
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;

public class BlockTypeHelper {
    public static boolean isCarpet(BlockType type) {
        if (type.blockShapeTypes().isEmpty()) {
            return false;
        }

        return type.blockShapeTypes().get(0).collisionHeight() <= 0.1;
    }

    public static boolean isReplaceable(BlockType type) {
        return isAir(type) || isFluid(type);
    }

    public static boolean isAir(BlockType type) {
        return type == BlockType.AIR
                || type == BlockType.CAVE_AIR
                || type == BlockType.VOID_AIR;
    }

    public static boolean isFluid(BlockType type) {
        return type == BlockType.WATER || type == BlockType.LAVA;
    }

    public static boolean isEmpty(BlockStateMeta meta) {
        return meta.blockShapeType().hasNoCollisions();
    }
}
