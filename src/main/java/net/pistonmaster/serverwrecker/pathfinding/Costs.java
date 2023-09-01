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
package net.pistonmaster.serverwrecker.pathfinding;

public class Costs {
    public static final double PRESET_COST = 2_000_000_000;
    public static final double INFINITY = 2_000_000_001;

    public static final double STRAIGHT = 1;
    public static final double DIAGONAL = 1.4142135623730951;
    public static final double FORWARD_FALL_ONE = 1.4142135623730951;
    public static final double FORWARD_FALL_TWO = 1.7320508075688772;
    public static final double FORWARD_FALL_THREE = 2.23606797749979;
    public static final double DIG_BLOCK = 2;
    public static final double PLACE_BLOCK = 1;

    private Costs() {
    }
}
