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

import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class ChunkData {
    private static final Map<Integer, ChunkSection> SECTION_CACHE = new WeakHashMap<>();
    private final LevelState level;
    private final ChunkSection[] sections;

    public ChunkData(LevelState level) {
        this.level = level;
        this.sections = new ChunkSection[level.getSectionsCount()];
    }

    public static int log2RoundUp(int num) {
        return (int) Math.ceil(Math.log(num) / Math.log(2));
    }

    public int getBlock(Vector3i block) {
        return getSection(block).getBlock(block.getX() & 0xF, block.getY() & 0xF, block.getZ() & 0xF);
    }

    private ChunkSection getSection(Vector3i block) {
        return getSection(level.getSectionIndex(block.getY()));
    }

    public ChunkSection getSection(int sectionIndex) {
        var section = sections[sectionIndex];
        Objects.requireNonNull(section, () -> "Section " + sectionIndex + " is null!");

        return section;
    }

    public int getSectionCount() {
        return sections.length;
    }

    private void setSection(Vector3i block, ChunkSection section) {
        setSection(level.getSectionIndex(block.getY()), section);
    }

    public void setSection(int sectionIndex, ChunkSection section) {
        var sectionHash = section.hashCode();
        synchronized (SECTION_CACHE) {
            SECTION_CACHE.compute(sectionHash, (hash, cachedSection) ->
                    sections[sectionIndex] = cachedSection == null ? section : cachedSection);
        }
    }

    public void setBlock(Vector3i block, int state) {
        var targetSection = getSection(block);
        var clone = new ChunkSection(
                targetSection.getBlockCount(),
                // Clone chunk data palette only
                new DataPalette(targetSection.getChunkData()),
                targetSection.getBiomeData()
        );
        clone.setBlock(block.getX() & 0xF, block.getY() & 0xF, block.getZ() & 0xF, state);

        setSection(block, clone);
    }
}
