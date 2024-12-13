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

import com.soulfiremc.server.util.SectionUtils;

public interface LevelHeightAccessor {
  int getHeight();

  int getMinY();

  default int getMaxY() {
    return this.getMinY() + this.getHeight() - 1;
  }

  default int getSectionsCount() {
    return this.getMaxSection() - this.getMinSection();
  }

  default int getMinSection() {
    return SectionUtils.blockToSection(this.getMinY());
  }

  default int getMaxSection() {
    return SectionUtils.blockToSection(this.getMaxY()) + 1;
  }

  default boolean isOutsideBuildHeight(int y) {
    return y < this.getMinY() || y > this.getMaxY();
  }

  default int getSectionIndex(int y) {
    return this.getSectionIndexFromSectionY(SectionUtils.blockToSection(y));
  }

  default int getSectionIndexFromSectionY(int sectionIndex) {
    return sectionIndex - this.getMinSection();
  }
}
