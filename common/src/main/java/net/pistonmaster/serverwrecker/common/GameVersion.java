/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.common;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum GameVersion {

    VERSION_1_7("1.7.4"),

    VERSION_1_8("1.8.9"),

    VERSION_1_9("1.9.4"),

    VERSION_1_10("1.10.2"),

    VERSION_1_11("1.11.2"),

    VERSION_1_12("1.12.2"),

    VERSION_1_13("1.13.2"),

    VERSION_1_14("1.14.4"),

    VERSION_1_15("1.15.2"),

    VERSION_1_16("1.16.5"),

    VERSION_1_17("1.17.1"),

    VERSION_1_18("1.18.1");

    private final String version;

    public static GameVersion findByName(String name) {
        for (GameVersion version : values()) {
            if (version.version.equals(name)) {
                return version;
            }
        }

        return null;
    }

    public static GameVersion getNewest() {
        return Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toUnmodifiableList()).get(0);
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return getVersion();
    }
}
