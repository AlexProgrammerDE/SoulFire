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
package com.soulfiremc.server.data;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("unused")
public record MapColor(int id, int col) {
  //@formatter:off
  // VALUES REPLACE
  //@formatter:on
  private static final MapColor EMPTY = COLORS[0];

  public static int getColorFromPackedId(int packedId) {
    var i = packedId & 0xFF;
    return COLORS[i >> 2].calculateRGBColor(Brightness.VALUES[i & 3]);
  }

  private static MapColor fromId(int id) {
    var mapColor = COLORS[id];
    return mapColor == null ? EMPTY : mapColor;
  }

  public int calculateRGBColor(Brightness brightness) {
    if (this == EMPTY) {
      return 0;
    } else {
      var m = brightness.modifier;
      var r = (this.col >> 16 & 0xFF) * m / 255;
      var g = (this.col >> 8 & 0xFF) * m / 255;
      var b = (this.col & 0xFF) * m / 255;
      return 0xFF000000 | b << 16 | g << 8 | r;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MapColor other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @RequiredArgsConstructor
  public enum Brightness {
    LOW(0, 180),
    NORMAL(1, 220),
    HIGH(2, 255),
    LOWEST(3, 135);

    public static final Brightness[] VALUES = values();
    public final int id;
    public final int modifier;
  }
}
