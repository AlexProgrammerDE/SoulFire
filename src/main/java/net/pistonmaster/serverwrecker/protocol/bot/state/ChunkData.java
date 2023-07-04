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
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Objects;

public class ChunkData {
    private final LevelState level;
    @Getter
    private final ChunkSection[] sections;

    public ChunkData(LevelState level) {
        this.level = level;
        this.sections = new ChunkSection[level.getSectionsCount()];
    }

    public static int log2RoundUp(int num) {
        return (int) Math.ceil(Math.log(num) / Math.log(2));
    }

    public void setBlock(Vector3i block, int state) {
        getSection(block).setBlock(block.getX() & 0xF, block.getY() & 0xF, block.getZ() & 0xF, state);
    }

    public int getBlock(Vector3i block) {
        return getSection(block).getBlock(block.getX() & 0xF, block.getY() & 0xF, block.getZ() & 0xF);
    }

    private ChunkSection getSection(Vector3i block) {
        return getSection(level.getSectionIndex(block.getY()));
    }

    private ChunkSection getSection(int sectionIndex) {
        ChunkSection section = sections[sectionIndex];
        Objects.requireNonNull(section, "Section " + sectionIndex + " is null!");

        return section;
    }
}
