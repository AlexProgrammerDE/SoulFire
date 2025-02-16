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

import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotPostEntityTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreEntityTickEvent;
import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
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
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Getter
public class Level implements LevelHeightAccessor {
  private final TickRateManager tickRateManager = new TickRateManager();
  private final EntityTrackerState entityTracker = new EntityTrackerState();
  private final BotConnection connection;
  private final TagsState tagsState;
  private final ChunkHolder chunks;
  private final DimensionType dimensionType;
  private final Key worldKey;
  private final long hashedSeed;
  private final boolean debug;
  private final int seaLevel;
  private final LevelData levelData;
  protected float oRainLevel;
  protected float rainLevel;
  protected float oThunderLevel;
  protected float thunderLevel;
  @Setter
  private BorderState borderState;
  private boolean tickDayTime;

  public Level(
    BotConnection connection,
    TagsState tagsState,
    DimensionType dimensionType,
    Key worldKey,
    long hashedSeed,
    boolean debug,
    int seaLevel,
    LevelData levelData) {
    this.connection = connection;
    this.tagsState = tagsState;
    this.dimensionType = dimensionType;
    this.worldKey = worldKey;
    this.hashedSeed = hashedSeed;
    this.debug = debug;
    this.seaLevel = seaLevel;
    this.levelData = levelData;

    this.chunks = new ChunkHolder(this);

    this.levelData.setSpawn(Vector3i.from(8, 64, 8), 0.0F);
    prepareWeather();
  }

  protected void prepareWeather() {
    if (this.levelData.raining()) {
      this.rainLevel = 1.0F;
    }
  }

  public void tick() {
    // Tick border changes
    if (borderState != null) {
      borderState.tick();
    }

    if (this.tickRateManager().runsNormally()) {
      this.tickTime();
    }
  }

  public void tickEntities() {
    SoulFireAPI.postEvent(new BotPreEntityTickEvent(connection));

    // Tick entities
    entityTracker.tick();

    SoulFireAPI.postEvent(new BotPostEntityTickEvent(connection));
  }

  private void tickTime() {
    this.levelData.gameTime = this.levelData.gameTime + 1L;
    if (this.tickDayTime) {
      this.levelData.dayTime = this.levelData.dayTime + 1L;
    }
  }

  public void setTimeFromServer(long gameTime, long dayTime, boolean tickDayTime) {
    this.levelData.gameTime = gameTime;
    this.levelData.dayTime = dayTime;
    this.tickDayTime = tickDayTime;
  }

  public float getThunderLevel(float delta) {
    return MathHelper.lerp(delta, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(delta);
  }

  public void setThunderLevel(float strength) {
    var clampedStrength = MathHelper.clamp(strength, 0.0F, 1.0F);
    this.oThunderLevel = clampedStrength;
    this.thunderLevel = clampedStrength;
  }

  public float getRainLevel(float delta) {
    return MathHelper.lerp(delta, this.oRainLevel, this.rainLevel);
  }

  public void setRainLevel(float strength) {
    var clampedStrength = MathHelper.clamp(strength, 0.0F, 1.0F);
    this.oRainLevel = clampedStrength;
    this.rainLevel = clampedStrength;
  }

  private boolean canHaveWeather() {
    return this.dimensionType().hasSkylight() && !this.dimensionType().hasCeiling();
  }

  public boolean isThundering() {
    return this.canHaveWeather() && (double) this.getThunderLevel(1.0F) > 0.9;
  }

  public boolean isRaining() {
    return this.canHaveWeather() && (double) this.getRainLevel(1.0F) > 0.2;
  }

  @Override
  public int getHeight() {
    return dimensionType.height();
  }

  @Override
  public int getMinY() {
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

  public Collection<Entity> getEntities() {
    return entityTracker.getEntities();
  }

  public Collection<Entity> getEntities(AABB bounds) {
    return getEntities()
      .stream()
      .filter(e -> e.getBoundingBox().intersects(bounds))
      .toList();
  }

  public List<Vector3i> getTouchedPositions(AABB aabb) {
    var startX = MathHelper.floor(aabb.minX - AABB.EPSILON) - 1;
    var endX = MathHelper.floor(aabb.maxX + AABB.EPSILON) + 1;
    var startY = MathHelper.floor(aabb.minY - AABB.EPSILON) - 1;
    var endY = MathHelper.floor(aabb.maxY + AABB.EPSILON) + 1;
    var startZ = MathHelper.floor(aabb.minZ - AABB.EPSILON) - 1;
    var endZ = MathHelper.floor(aabb.maxZ + AABB.EPSILON) + 1;

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

  public List<AABB> getEntityCollisions(Entity entity, AABB aabb) {
    if (aabb.getSize() < 1.0E-7) {
      return List.of();
    } else {
      var collisionBOx = aabb.inflate(1.0E-7);

      return getEntities().stream()
        .filter(e -> e != entity)
        .filter(entity::canCollideWith)
        .map(Entity::getBoundingBox)
        .filter(e -> e.intersects(collisionBOx))
        .toList();
    }
  }

  @Getter
  @Setter
  public static class LevelData {
    private final boolean hardcore;
    private final boolean isFlat;
    private Vector3i spawnPos;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    private boolean raining;
    private Difficulty difficulty;
    private boolean difficultyLocked;

    public LevelData(Difficulty difficulty, boolean hardcore, boolean flat) {
      this.difficulty = difficulty;
      this.hardcore = hardcore;
      this.isFlat = flat;
    }

    public void setSpawn(Vector3i spawnPoint, float spawnAngle) {
      this.spawnPos = spawnPoint;
      this.spawnAngle = spawnAngle;
    }
  }
}
