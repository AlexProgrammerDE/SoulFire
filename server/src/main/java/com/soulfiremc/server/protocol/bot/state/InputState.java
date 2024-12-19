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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputState {
  private final ControlState state;
  public KeyPresses keyPresses = KeyPresses.EMPTY;
  public float leftImpulse;
  public float forwardImpulse;

  private static float calculateImpulse(boolean input, boolean otherInput) {
    if (input == otherInput) {
      return 0.0F;
    } else {
      return input ? 1.0F : -1.0F;
    }
  }

  public boolean hasForwardImpulse() {
    return this.forwardImpulse > 1.0E-5F;
  }

  public void makeJump() {
    this.keyPresses = new KeyPresses(
      this.keyPresses.forward(),
      this.keyPresses.backward(),
      this.keyPresses.left(),
      this.keyPresses.right(),
      true,
      this.keyPresses.shift(),
      this.keyPresses.sprint()
    );
  }

  public void tick(boolean movingSlowly, float amount) {
    this.keyPresses = state.toKeyPresses();
    this.forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
    this.leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
    if (movingSlowly) {
      this.leftImpulse *= amount;
      this.forwardImpulse *= amount;
    }
  }
}
