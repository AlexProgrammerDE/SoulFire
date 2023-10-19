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
package net.pistonmaster.serverwrecker.protocol.bot.state;

import com.github.steveice10.opennbt.tag.builtin.*;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.nbt.MCUniform;
import net.pistonmaster.serverwrecker.protocol.bot.nbt.UniformOrInt;
import net.pistonmaster.serverwrecker.protocol.bot.utils.SectionUtils;
import net.pistonmaster.serverwrecker.util.BoundingBox;
import net.pistonmaster.serverwrecker.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class LevelState {
    private final SessionDataManager sessionDataManager;
    private final ChunkHolder chunks = new ChunkHolder(this);
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

    // Some precalculated chunk values based on level data
    private final int minSection;
    private final int maxSection;
    private final int sectionsCount;
    @Setter
    private long worldAge;
    @Setter
    private long time;

    public LevelState(SessionDataManager sessionDataManager, String dimensionName, int dimensionId, CompoundTag levelRegistry) {
        this.sessionDataManager = sessionDataManager;
        this.dimensionName = dimensionName;
        this.dimensionId = dimensionId;
        Object lightLevel = levelRegistry.get("monster_spawn_light_level");
        if (lightLevel instanceof CompoundTag lightCompound) {
            this.monsterSpawnLightLevel = new MCUniform(lightCompound.get("value"));
        } else if (lightLevel instanceof IntTag lightInt) {
            this.monsterSpawnLightLevel = new MCUniform(lightInt.getValue(), lightInt.getValue());
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
        this.monsterSpawnBlockLightLimit = levelRegistry.<IntTag>get("monster_spawn_block_light_limit").getValue();
        this.hasRaids = levelRegistry.<ByteTag>get("has_raids").getValue();
        this.respawnAnchorWorks = levelRegistry.<ByteTag>get("respawn_anchor_works").getValue();

        // Precalculate min section
        this.minSection = SectionUtils.blockToSection(this.getMinBuildHeight());
        this.maxSection = SectionUtils.blockToSection(this.getMaxBuildHeight() - 1) + 1;
        this.sectionsCount = this.maxSection - this.minSection;
    }

    public int getMinBuildHeight() {
        return this.minY;
    }

    public int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.height;
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

    public Optional<BlockStateMeta> getBlockStateAt(Vector3i block) {
        return chunks.getBlockStateAt(block);
    }

    public Optional<BlockType> getBlockTypeAt(Vector3i block) {
        return getBlockStateAt(block).map(BlockStateMeta::blockType);
    }

    public boolean isOutOfWorld(Vector3i block) {
        return block.getY() < this.getMinBuildHeight() || block.getY() >= this.getMaxBuildHeight();
    }

    public List<BoundingBox> getCollisionBoxes(BoundingBox aabb) {
        var boundingBoxList = new ArrayList<BoundingBox>();

        var minX = MathHelper.floorDouble(aabb.minX);
        var maxX = MathHelper.floorDouble(aabb.maxX + 1.0);
        var minY = MathHelper.floorDouble(aabb.minY);
        var maxY = MathHelper.floorDouble(aabb.maxY + 1.0);
        var minZ = MathHelper.floorDouble(aabb.minZ);
        var maxZ = MathHelper.floorDouble(aabb.maxZ + 1.0);

        for (var x = minX; x < maxX; x++) {
            for (var y = minY; y < maxY; y++) {
                for (var z = minZ; z < maxZ; z++) {
                    var block = Vector3i.from(x, y, z);
                    if (isOutOfWorld(block)) {
                        continue;
                    }

                    var blockState = getBlockStateAt(block);
                    if (blockState.isEmpty()) {
                        continue;
                    }

                    var blockShapeType = blockState.get().blockShapeType();
                    if (blockShapeType.hasNoCollisions()) {
                        continue;
                    }

                    for (var shape : blockShapeType.blockShapes()) {
                        var boundingBox = shape.createBoundingBoxAt(x, y, z);
                        if (boundingBox.intersects(aabb)) {
                            boundingBoxList.add(boundingBox);
                        }
                    }
                }
            }
        }

        return boundingBoxList;
    }
}
