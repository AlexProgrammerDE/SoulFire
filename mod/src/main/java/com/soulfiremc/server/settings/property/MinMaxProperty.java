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

import com.soulfiremc.server.settings.lib.SettingsSource;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract non-sealed class MinMaxProperty<S extends SettingsSource> implements Property<S> {
  public abstract String namespace();

  public abstract String key();

  public abstract int minValue();

  public abstract int maxValue();

  @Value.Default
  public int stepValue() {
    return 1;
  }

  @Value.Default
  public boolean thousandSeparator() {
    return true;
  }

  public abstract MinMaxPropertyEntry minEntry();

  public abstract MinMaxPropertyEntry maxEntry();

  @Value.Default
  public boolean disabled() {
    return false;
  }

  public DataLayout defaultDataLayout() {
    return new DataLayout(minEntry().defaultValue(), maxEntry().defaultValue());
  }

  public record DataLayout(int min, int max) {}
}
