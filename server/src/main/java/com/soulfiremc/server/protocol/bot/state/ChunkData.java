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

import com.soulfiremc.server.protocol.bot.utils.SectionUtils;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

public class ChunkData {
  private static final Map<ChunkSection, ChunkSection> SECTION_CACHE = new WeakHashMap<>();
  private final int minSection;
  private final ChunkSection[] sections;

  public ChunkData(int minSection, int sectionsCount, boolean fillEmpty) {
    this.minSection = minSection;
    this.sections = new ChunkSection[sectionsCount];
    if (fillEmpty) {
      fillEmpty();
    }
  }

  public static int log2RoundUp(int num) {
    return (int) Math.ceil(Math.log(num) / Math.log(2));
  }

  public void fillEmpty() {
    for (var i = 0; i < sections.length; i++) {
      setSection(i, new ChunkSection());
    }
  }

  public int getBlock(int x, int y, int z) {
    return getSection(getSectionIndexByBlockY(y)).getBlock(x & 0xF, y & 0xF, z & 0xF);
  }

  public ChunkSection getSection(int sectionIndex) {
    var section = sections[sectionIndex];
    if (section == null) {
      throw new NullPointerException("Section %d is null!".formatted(sectionIndex));
    }

    return section;
  }

  public boolean isSectionMissing(int sectionIndex) {
    return sections[sectionIndex] == null;
  }

  public int getSectionCount() {
    return sections.length;
  }

  public void setSection(int sectionIndex, ChunkSection section) {
    synchronized (SECTION_CACHE) {
      sections[sectionIndex] = SECTION_CACHE.computeIfAbsent(section, Function.identity());
    }
  }

  public void setBlock(Vector3i block, int state) {
    var y = block.getY();
    var sectionIndex = getSectionIndexByBlockY(y);
    var targetSection = getSection(sectionIndex);
    var clone =
      new ChunkSection(
        targetSection.getBlockCount(),
        // Clone chunk data palette only
        new DataPalette(targetSection.getChunkData()),
        targetSection.getBiomeData());
    clone.setBlock(block.getX() & 0xF, y & 0xF, block.getZ() & 0xF, state);

    setSection(sectionIndex, clone);
  }

  private int getSectionIndexByBlockY(int blockY) {
    return SectionUtils.blockToSection(blockY) - this.minSection;
  }
}
