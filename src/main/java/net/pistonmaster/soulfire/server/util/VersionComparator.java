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
package net.pistonmaster.soulfire.server.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class VersionComparator {
    private VersionComparator() {
    }

    public static boolean isNewer(String currentVersion, String checkVersion) {
        currentVersion = currentVersion.replace("-SNAPSHOT", "");
        checkVersion = checkVersion.replace("-SNAPSHOT", "");

        try {
            var currentVersionData = Arrays.stream(currentVersion.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();
            var checkVersionData = Arrays.stream(checkVersion.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();

            var i = 0;
            for (var version : checkVersionData) {
                if (i == currentVersionData.length) {
                    return true;
                }

                if (version > currentVersionData[i]) {
                    return true;
                } else if (version < currentVersionData[i]) {
                    return false;
                }

                i++;
            }
        } catch (NumberFormatException e) {
            log.error("Error while parsing version!", e);
        }
        return false;
    }
}
