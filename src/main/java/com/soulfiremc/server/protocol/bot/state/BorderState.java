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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class BorderState {
  private double centerX;
  private double centerZ;
  private double oldSize;
  private double newSize;
  private long lerpTime;
  private int newAbsoluteMaxSize;
  private int warningBlocks;
  private int warningTime;

  public void tick() {
    if (lerpTime > 0) {
      var d = (double) (System.currentTimeMillis() - lerpTime) / 1000.0D;

      if (d < 0.0D || d > 1.0D) {
        lerpTime = 0;
        oldSize = newSize;
      } else {
        oldSize = oldSize + (newSize - oldSize) * d;
      }
    }
  }
}
