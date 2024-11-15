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
package com.soulfiremc.server.settings.property;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(stagedBuilder = true)
public non-sealed abstract class MinMaxProperty implements Property {
  public abstract String namespace();

  public abstract String key();

  public abstract String minUiName();

  public abstract String maxUiName();

  public abstract String minDescription();

  public abstract String maxDescription();

  public abstract int minDefaultValue();

  public abstract int maxDefaultValue();

  public abstract int minValue();

  public abstract int maxValue();

  public abstract int stepValue();

  public abstract Optional<String> format();

  public DataLayout defaultDataLayout() {
    return new DataLayout(minDefaultValue(), maxDefaultValue());
  }

  public record DataLayout(int min, int max) {}
}
