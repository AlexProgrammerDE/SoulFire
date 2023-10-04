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

public class MathHelper {
    private MathHelper() {
    }

    /**
     * Returns the greatest integer less than or equal to the double argument
     *
     * @param value A double
     * @return The greatest integer less than or equal to the double argument
     */
    public static int floorDouble(double value) {
        var i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    public static float wrapDegrees(float v) {
        v %= 360.0F;

        if (v >= 180.0F) {
            v -= 360.0F;
        }

        if (v < -180.0F) {
            v += 360.0F;
        }

        return v;
    }

    public static short clamp(short value, short min, short max) {
        return value < min ? min : (value > max ? max : value);
    }
}
