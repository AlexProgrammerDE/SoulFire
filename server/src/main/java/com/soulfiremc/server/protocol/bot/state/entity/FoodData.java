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
package com.soulfiremc.server.protocol.bot.state.entity;

public class FoodData {
  private int foodLevel = 20;
  private float saturationLevel = 5.0F;

  public int getFoodLevel() {
    return this.foodLevel;
  }

  public void setFoodLevel(int foodLevel) {
    this.foodLevel = foodLevel;
  }

  public boolean hasEnoughFood() {
    return this.foodLevel >= 20;
  }

  public boolean needsFood() {
    return this.foodLevel < 20;
  }

  public float getSaturationLevel() {
    return this.saturationLevel;
  }

  public void setSaturation(float saturationLevel) {
    this.saturationLevel = saturationLevel;
  }
}
