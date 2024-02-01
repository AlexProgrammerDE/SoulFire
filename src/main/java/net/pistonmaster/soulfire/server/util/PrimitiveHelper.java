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

import java.util.Arrays;

public class PrimitiveHelper {
    public static byte[][] split(byte[] data, byte separator) {
        int count = 0;
        for (byte b : data) {
            if (b == separator) {
                count++;
            }
        }

        if (count == 0) {
            return new byte[][]{data};
        }

        byte[][] result = new byte[count + 1][];
        int last = 0;
        int index = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == separator) {
                result[index++] = Arrays.copyOfRange(data, last, i);
                last = i + 1;
            }
        }

        result[index] = Arrays.copyOfRange(data, last, data.length);
        return result;
    }
}
