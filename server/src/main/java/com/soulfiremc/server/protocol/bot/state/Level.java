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
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.protocol.bot.state.registry.DimensionType;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.structs.TickRateManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
  private final int seaLevel;

  @Setter
  private long gameTime;
  @Setter
  private long dayTime;
  @Setter
  private boolean tickDayTime;

  public Level(
    TagsState tagsState,
    DimensionType dimensionType,
    Key worldKey,
    long hashedSeed,
    boolean debug,
    boolean flat,
    int seaLevel) {
    this.tagsState = tagsState;
    this.dimensionType = dimensionType;
    this.worldKey = worldKey;
    this.hashedSeed = hashedSeed;
    this.debug = debug;
    this.flat = flat;
    this.seaLevel = seaLevel;

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

  public boolean isChunkPositionLoaded(int blockX, int blockZ) {
    return chunks.isChunkPositionLoaded(blockX, blockZ);
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

  public List<Vector3i> getTouchedPositions(AABB aabb) {
    var startX = MathHelper.floorDouble(aabb.minX - AABB.EPSILON) - 1;
    var endX = MathHelper.floorDouble(aabb.maxX + AABB.EPSILON) + 1;
    var startY = MathHelper.floorDouble(aabb.minY - AABB.EPSILON) - 1;
    var endY = MathHelper.floorDouble(aabb.maxY + AABB.EPSILON) + 1;
    var startZ = MathHelper.floorDouble(aabb.minZ - AABB.EPSILON) - 1;
    var endZ = MathHelper.floorDouble(aabb.maxZ + AABB.EPSILON) + 1;

    var predictedSize = (endX - startX + 1) * (endY - startY + 1) * (endZ - startZ + 1);
    var surroundingBlocks = new ArrayList<Vector3i>(predictedSize);

    for (var x = startX; x <= endX; x++) {
      for (var y = startY; y <= endY; y++) {
        for (var z = startZ; z <= endZ; z++) {
          surroundingBlocks.add(Vector3i.from(x, y, z));
        }
      }
    }

    return surroundingBlocks;
  }

  public List<AABB> getBlockCollisionBoxes(AABB aabb) {
    return getTouchedPositions(aabb).stream()
      .flatMap(cursor -> getBlockState(cursor).getCollisionBoxes(cursor).stream())
      .filter(collisionBox -> collisionBox.intersects(aabb))
      .toList();
  }

  public boolean containsAnyLiquid(AABB bb) {
    var minX = MathHelper.floor(bb.minX);
    var maxX = MathHelper.ceil(bb.maxX);
    var minY = MathHelper.floor(bb.minY);
    var maxY = MathHelper.ceil(bb.maxY);
    var minZ = MathHelper.floor(bb.minZ);
    var maxZ = MathHelper.ceil(bb.maxZ);

    for (var x = minX; x < maxX; x++) {
      for (var y = minY; y < maxY; y++) {
        for (var z = minZ; z < maxZ; z++) {
          var blockState = this.getBlockState(Vector3i.from(x, y, z));
          if (!blockState.fluidState().empty()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public boolean noCollision(AABB bb) {
    return getBlockCollisionBoxes(bb).isEmpty();
  }

  public Optional<Vector3i> findSupportingBlock(Entity entity, AABB bb) {
    Vector3i block = null;
    var distance = Double.MAX_VALUE;

    for (var position : getTouchedPositions(bb)) {
      var distanceToCenter = VectorHelper.distToCenterSqr(position, entity.pos());
      if (distanceToCenter < distance || distanceToCenter == distance && (block == null || block.compareTo(position) < 0)) {
        block = position;
        distance = distanceToCenter;
      }
    }

    return Optional.ofNullable(block);
  }
}
