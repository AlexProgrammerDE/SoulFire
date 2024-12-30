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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Value.Immutable
@Value.Style(stagedBuilder = true)
public non-sealed abstract class ComboProperty implements Property {
  public static <T extends Enum<T>> ComboOption[] optionsFromEnum(
    T[] values, Function<T, String> mapper) {
    var options = new ComboOption[values.length];

    for (var i = 0; i < values.length; i++) {
      options[i] = new ComboOption(values[i].name(), mapper.apply(values[i]));
    }

    return options;
  }

  private static String capitalizeString(String str) {
    return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
  }

  public static <T extends Enum<T>> String capitalizeEnum(T enumValue) {
    return String.join(
      " ",
      Arrays.stream(enumValue.name().split("_"))
        .map(ComboProperty::capitalizeString)
        .toArray(String[]::new));
  }

  public abstract String key();

  public abstract String uiName();

  public abstract String description();

  public abstract List<ComboOption> options();

  public abstract String defaultValue();

  @Value.Check
  protected void check() {
    if (options().isEmpty()) {
      throw new IllegalArgumentException("Options must not be empty!");
    }

    if (options().stream().noneMatch(option -> option.id().equals(defaultValue()))) {
      throw new IllegalArgumentException("Default value must be in range of options!");
    }
  }

  public record ComboOption(String id, String displayName) {
  }
}
