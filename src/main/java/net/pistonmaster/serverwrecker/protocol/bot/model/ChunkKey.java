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
package net.pistonmaster.serverwrecker.protocol.bot.model;

import net.pistonmaster.serverwrecker.protocol.bot.utils.SectionUtils;
import org.cloudburstmc.math.vector.Vector3i;

public record ChunkKey(int chunkX, int chunkZ, int calculatedHash) {
    public ChunkKey(int chunkX, int chunkZ) {
        this(chunkX, chunkZ, calculateHash(chunkX, chunkZ));
    }

    public static int calculateHash(Vector3i block) {
        return calculateHash(SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
    }

    public static int calculateHash(int chunkX, int chunkZ) {
        return (chunkX << 16) | (chunkZ & 0xFFFF);
    }

    @Override
    public int hashCode() {
        return calculatedHash;
    }
}
