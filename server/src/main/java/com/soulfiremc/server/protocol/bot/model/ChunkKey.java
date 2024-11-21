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
package com.soulfiremc.server.protocol.bot.model;

import com.soulfiremc.server.util.SectionUtils;
import org.cloudburstmc.math.vector.Vector3i;

public record ChunkKey(int chunkX, int chunkZ) {
  public static final ChunkKey ZERO = new ChunkKey(0, 0);

  public static ChunkKey fromBlock(Vector3i block) {
    return new ChunkKey(SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
  }

  public static long calculateKey(int chunkX, int chunkZ) {
    return (long) chunkX & 0xffffffffL | ((long) chunkZ & 0xffffffffL) << 32;
  }

  public static ChunkKey fromKey(long key) {
    return new ChunkKey((int) key, (int) (key >> 32));
  }
}
