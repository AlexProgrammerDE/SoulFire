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

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.opennbt.tag.builtin.FloatTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
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
    CompoundTag levelRegistry) {
    this.tagsState = tagsState;
    this.dimensionName = dimensionName;
    this.dimensionId = dimensionId;
    Object lightLevel = levelRegistry.get("monster_spawn_light_level");
    if (lightLevel instanceof CompoundTag lightCompound) {
      this.monsterSpawnLightLevel = new MCUniform(lightCompound.get("value"));
    } else if (lightLevel instanceof IntTag lightInt) {
      this.monsterSpawnLightLevel = new MCUniformInt(lightInt.getValue());
    } else {
      throw new IllegalArgumentException("Invalid monster_spawn_light_level: " + lightLevel);
    }

    this.infiniburn = levelRegistry.<StringTag>get("infiniburn").getValue();
    this.effects = levelRegistry.<StringTag>get("effects").getValue();
    this.ultrawarm = levelRegistry.<ByteTag>get("ultrawarm").getValue();
    this.height = levelRegistry.<IntTag>get("height").getValue();
    this.logicalHeight = levelRegistry.<IntTag>get("logical_height").getValue();
    this.natural = levelRegistry.<ByteTag>get("natural").getValue();
    this.minY = levelRegistry.<IntTag>get("min_y").getValue();
    this.bedWorks = levelRegistry.<ByteTag>get("bed_works").getValue();
    LongTag fixedTimeTad = levelRegistry.get("fixed_time");
    this.fixedTime = fixedTimeTad == null ? null : fixedTimeTad.getValue();
    this.coordinateScale = levelRegistry.<DoubleTag>get("coordinate_scale").getValue();
    this.piglinSafe = levelRegistry.<ByteTag>get("piglin_safe").getValue();
    this.hasCeiling = levelRegistry.<ByteTag>get("has_ceiling").getValue();
    this.hasSkylight = levelRegistry.<ByteTag>get("has_skylight").getValue();
    this.ambientLight = levelRegistry.<FloatTag>get("ambient_light").getValue();
    this.monsterSpawnBlockLightLimit =
      levelRegistry.<IntTag>get("monster_spawn_block_light_limit").getValue();
    this.hasRaids = levelRegistry.<ByteTag>get("has_raids").getValue();
    this.respawnAnchorWorks = levelRegistry.<ByteTag>get("respawn_anchor_works").getValue();

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
