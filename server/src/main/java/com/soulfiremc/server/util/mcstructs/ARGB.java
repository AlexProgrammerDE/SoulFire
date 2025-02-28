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
package com.soulfiremc.server.util.mcstructs;

import com.soulfiremc.server.util.MathHelper;

public class ARGB {
  public static int alpha(int color) {
    return color >>> 24;
  }

  public static int red(int color) {
    return color >> 16 & 0xFF;
  }

  public static int green(int color) {
    return color >> 8 & 0xFF;
  }

  public static int blue(int color) {
    return color & 0xFF;
  }

  public static int color(int alpha, int red, int green, int blue) {
    return alpha << 24 | red << 16 | green << 8 | blue;
  }

  public static int lerp(float delta, int color1, int color2) {
    var alpha = MathHelper.lerpInt(delta, alpha(color1), alpha(color2));
    var red = MathHelper.lerpInt(delta, red(color1), red(color2));
    var green = MathHelper.lerpInt(delta, green(color1), green(color2));
    var blue = MathHelper.lerpInt(delta, blue(color1), blue(color2));
    return color(alpha, red, green, blue);
  }
}
