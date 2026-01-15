/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.settings.property;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract non-sealed class DoubleProperty implements Property {
  public abstract String key();

  public abstract String uiName();

  public abstract String description();

  public abstract double defaultValue();

  public abstract double minValue();

  public abstract double maxValue();

  @Value.Default
  public double stepValue() {
    return 1;
  }

  @Value.Default
  public String placeholder() {
    return "";
  }

  @Value.Default
  public boolean thousandSeparator() {
    return true;
  }

  @Value.Default
  public int decimalScale() {
    return 2;
  }

  @Value.Default
  public boolean fixedDecimalScale() {
    return true;
  }

  @Value.Default
  public boolean disabled() {
    return false;
  }
}
