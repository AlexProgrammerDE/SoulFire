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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ControlState {
  private boolean forward;
  private boolean backward;
  private boolean left;
  private boolean right;
  private boolean jumping;
  private boolean sneaking;
  private boolean sprinting;
  private boolean flying;

  public void resetWasd() {
    forward = false;
    backward = false;
    left = false;
    right = false;
  }

  public void resetAll() {
    resetWasd();
    sprinting = false;
    jumping = false;
    sneaking = false;
    flying = false;
  }

  public KeyPresses toKeyPresses() {
    return new KeyPresses(forward, backward, left, right, jumping, sneaking, sprinting);
  }
}
