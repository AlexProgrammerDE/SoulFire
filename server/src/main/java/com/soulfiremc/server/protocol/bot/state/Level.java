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
import com.soulfiremc.server.protocol.bot.nbt.MCUniform;
import com.soulfiremc.server.protocol.bot.nbt.MCUniformInt;
import com.soulfiremc.server.protocol.bot.nbt.UniformOrInt;
import com.soulfiremc.server.util.MathHelper;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;

@Getter
public class Level {
  private final TagsState tagsState;
  private final ChunkHolder chunks;
  private final String dimensionName;
  private final int dimensionId;
  private final UniformOrInt monsterSpawnLightLevel;
  private final String infiniburn;
  private final String effects;
  private final byte ultrawarm;
  @Getter
  private final int height;
  private final int logicalHeight;
  private final byte natural;
  private final int minY;
  private final byte bedWorks;
  private final @Nullable Long fixedTime; // Only nether and end
  private final double coordinateScale;
  private final byte piglinSafe;
  private final byte hasCeiling;
  private final byte hasSkylight;
  private final float ambientLight;
  private final int monsterSpawnBlockLightLimit;
  private final byte hasRaids;
  private final byte respawnAnchorWorks;

  @Setter
  private long worldAge;
  @Setter
  private long time;

  public Level(
    TagsState tagsState,
    String dimensionName,
    int dimensionId,
    NbtMap levelRegistry) {
    this.tagsState = tagsState;
    this.dimensionName = dimensionName;
    this.dimensionId = dimensionId;
    var lightLevel = levelRegistry.get("monster_spawn_light_level");
    if (lightLevel instanceof NbtMap lightCompound) {
      this.monsterSpawnLightLevel = new MCUniform(lightCompound.getCompound("value"));
    } else if (lightLevel instanceof Integer lightInt) {
      this.monsterSpawnLightLevel = new MCUniformInt(lightInt);
    } else {
      throw new IllegalArgumentException("Invalid monster_spawn_light_level: " + lightLevel);
    }

    this.infiniburn = levelRegistry.getString("infiniburn");
    this.effects = levelRegistry.getString("effects");
    this.ultrawarm = levelRegistry.getByte("ultrawarm");
    this.height = levelRegistry.getInt("height");
    this.logicalHeight = levelRegistry.getInt("logical_height");
    this.natural = levelRegistry.getByte("natural");
    this.minY = levelRegistry.getInt("min_y");
    this.bedWorks = levelRegistry.getByte("bed_works");
    this.fixedTime = levelRegistry.containsKey("fixed_time") ? levelRegistry.getLong("fixed_time") : null;
    this.coordinateScale = levelRegistry.getDouble("coordinate_scale");
    this.piglinSafe = levelRegistry.getByte("piglin_safe");
    this.hasCeiling = levelRegistry.getByte("has_ceiling");
    this.hasSkylight = levelRegistry.getByte("has_skylight");
    this.ambientLight = levelRegistry.getFloat("ambient_light");
    this.monsterSpawnBlockLightLimit =
      levelRegistry.getInt("monster_spawn_block_light_limit");
    this.hasRaids = levelRegistry.getByte("has_raids");
    this.respawnAnchorWorks = levelRegistry.getByte("respawn_anchor_works");

    this.chunks = new ChunkHolder(getMinBuildHeight(), getMaxBuildHeight());
  }

  public int getMinBuildHeight() {
    return this.minY;
  }

  public int getMaxBuildHeight() {
    return this.getMinBuildHeight() + this.height;
  }

  public boolean isOutSideBuildHeight(SFVec3i block) {
    return isOutSideBuildHeight(block.y);
  }

  public boolean isOutSideBuildHeight(double y) {
    return y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight();
  }

  public void setBlockId(Vector3i block, int state) {
    var chunkData = chunks.getChunk(block);

    // Ignore block updates for unloaded chunks; that's what vanilla does.
    if (chunkData == null) {
      return;
    }

    chunkData.setBlock(block, state);
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
