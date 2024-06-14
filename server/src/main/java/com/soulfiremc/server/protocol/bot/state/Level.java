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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.bot.movement.AABB;
import com.soulfiremc.server.protocol.bot.state.registry.DimensionType;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.TickRateManager;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
public class Level implements LevelHeightAccessor {
  private final TickRateManager tickRateManager = new TickRateManager();
  private final TagsState tagsState;
  private final ChunkHolder chunks;
  private final DimensionType dimensionType;
  private final Key worldKey;
  private final long hashedSeed;
  private final boolean debug;
  private final boolean flat;

  @Setter
  private long worldAge;
  @Setter
  private long time;

  public Level(
    TagsState tagsState,
    DimensionType dimensionType,
    Key worldKey,
    long hashedSeed,
    boolean debug,
    boolean flat) {
    this.tagsState = tagsState;
    this.dimensionType = dimensionType;
    this.worldKey = worldKey;
    this.hashedSeed = hashedSeed;
    this.debug = debug;
    this.flat = flat;

    this.chunks = new ChunkHolder(this);
  }

  @Override
  public int getHeight() {
    return dimensionType.height();
  }

  @Override
  public int getMinBuildHeight() {
    return dimensionType.minY();
  }

  public void setBlock(Vector3i block, BlockState state) {
    setBlockId(block, state.id());
  }

  public void setBlockId(Vector3i block, int stateId) {
    var chunkData = chunks.getChunk(block);

    // Ignore block updates for unloaded chunks; that's what vanilla does.
    if (chunkData == null) {
      return;
    }

    chunkData.setBlock(block, stateId);
  }

  public boolean isChunkLoaded(Vector3i block) {
    return chunks.isChunkLoaded(block);
  }

  public BlockState getBlockState(Vector3i block) {
    return getBlockState(block.getX(), block.getY(), block.getZ());
  }

  public BlockState getBlockState(SFVec3i block) {
    return getBlockState(block.x, block.y, block.z);
  }

  public BlockState getBlockState(int x, int y, int z) {
    return chunks.getBlockState(x, y, z);
  }

  public List<AABB> getCollisionBoxes(AABB aabb) {
    var startX = MathHelper.floorDouble(aabb.minX - 1.0E-7) - 1;
    var endX = MathHelper.floorDouble(aabb.maxX + 1.0E-7) + 1;
    var startY = MathHelper.floorDouble(aabb.minY - 1.0E-7) - 1;
    var endY = MathHelper.floorDouble(aabb.maxY + 1.0E-7) + 1;
    var startZ = MathHelper.floorDouble(aabb.minZ - 1.0E-7) - 1;
    var endZ = MathHelper.floorDouble(aabb.maxZ + 1.0E-7) + 1;

    var predictedSize = (endX - startX + 1) * (endY - startY + 1) * (endZ - startZ + 1);
    var surroundingBBs = new ArrayList<AABB>(predictedSize);

    for (var x = startX; x <= endX; x++) {
      for (var y = startY; y <= endY; y++) {
        for (var z = startZ; z <= endZ; z++) {
          var cursor = Vector3i.from(x, y, z);
          var blockState = getBlockState(cursor);

          for (var collisionBox : blockState.getCollisionBoxes(cursor)) {
            if (collisionBox.intersects(aabb)) {
              surroundingBBs.add(collisionBox);
            }
          }
        }
      }
    }

    return surroundingBBs;
  }
}
