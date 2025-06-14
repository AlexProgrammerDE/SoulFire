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
package com.soulfiremc.server.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public final class ControlState {
  private boolean up;
  private boolean down;
  private boolean left;
  private boolean right;
  private boolean jump;
  private boolean shift;
  private boolean sprint;

  public void resetWasd() {
    up = false;
    down = false;
    left = false;
    right = false;
  }

  public void resetAll() {
    resetWasd();
    sprint = false;
    jump = false;
    shift = false;
  }
}
