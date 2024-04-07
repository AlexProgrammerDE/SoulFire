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
  public static final MapColor[] COLORS = new MapColor[] {
    new MapColor(0, 0),
    new MapColor(1, 8368696),
    new MapColor(2, 16247203),
    new MapColor(3, 13092807),
    new MapColor(4, 16711680),
    new MapColor(5, 10526975),
    new MapColor(6, 10987431),
    new MapColor(7, 31744),
    new MapColor(8, 16777215),
    new MapColor(9, 10791096),
    new MapColor(10, 9923917),
    new MapColor(11, 7368816),
    new MapColor(12, 4210943),
    new MapColor(13, 9402184),
    new MapColor(14, 16776437),
    new MapColor(15, 14188339),
    new MapColor(16, 11685080),
    new MapColor(17, 6724056),
    new MapColor(18, 15066419),
    new MapColor(19, 8375321),
    new MapColor(20, 15892389),
    new MapColor(21, 5000268),
    new MapColor(22, 10066329),
    new MapColor(23, 5013401),
    new MapColor(24, 8339378),
    new MapColor(25, 3361970),
    new MapColor(26, 6704179),
    new MapColor(27, 6717235),
    new MapColor(28, 10040115),
    new MapColor(29, 1644825),
    new MapColor(30, 16445005),
    new MapColor(31, 6085589),
    new MapColor(32, 4882687),
    new MapColor(33, 55610),
    new MapColor(34, 8476209),
    new MapColor(35, 7340544),
    new MapColor(36, 13742497),
    new MapColor(37, 10441252),
    new MapColor(38, 9787244),
    new MapColor(39, 7367818),
    new MapColor(40, 12223780),
    new MapColor(41, 6780213),
    new MapColor(42, 10505550),
    new MapColor(43, 3746083),
    new MapColor(44, 8874850),
    new MapColor(45, 5725276),
    new MapColor(46, 8014168),
    new MapColor(47, 4996700),
    new MapColor(48, 4993571),
    new MapColor(49, 5001770),
    new MapColor(50, 9321518),
    new MapColor(51, 2430480),
    new MapColor(52, 12398641),
    new MapColor(53, 9715553),
    new MapColor(54, 6035741),
    new MapColor(55, 1474182),
    new MapColor(56, 3837580),
    new MapColor(57, 5647422),
    new MapColor(58, 1356933),
    new MapColor(59, 6579300),
    new MapColor(60, 14200723),
    new MapColor(61, 8365974),
    null,
    null
  };
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
