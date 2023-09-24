/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.util;

import java.util.Arrays;

public class VersionComparator {
    private VersionComparator() {
    }

    public static boolean isNewer(String currentVersion, String checkVersion) {
        currentVersion = currentVersion.replace("-SNAPSHOT", "");
        checkVersion = checkVersion.replace("-SNAPSHOT", "");

        try {
            int[] currentVersionData = Arrays.stream(currentVersion.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();
            int[] checkVersionData = Arrays.stream(checkVersion.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();

            int i = 0;
            for (int version : checkVersionData) {
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
            e.printStackTrace();
        }
        return false;
    }
}
