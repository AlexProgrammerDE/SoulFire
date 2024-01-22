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
package net.pistonmaster.soulfire.server.settings.lib.property;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public record ComboProperty(
        String namespace,
        String key,
        String uiName,
        String[] cliFlags,
        String description,
        ComboOption[] options,
        int defaultValue
) implements SingleProperty {
    public static <T extends Enum<T>> String capitalizeEnum(T enumValue) {
        return String.join(" ", Arrays.stream(enumValue.name().split("_"))
                .map(ComboProperty::capitalizeString)
                .toArray(String[]::new));
    }

    public static String capitalizeString(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }

    public record ComboOption(
            String id,
            String displayName
    ) {
        public static <T extends Enum<T>> ComboOption[] fromEnum(T[] values, Function<T, String> mapper) {
            var options = new ComboOption[values.length];

            for (var i = 0; i < values.length; i++) {
                options[i] = new ComboOption(values[i].name(), mapper.apply(values[i]));
            }

            return options;
        }
    }
}
