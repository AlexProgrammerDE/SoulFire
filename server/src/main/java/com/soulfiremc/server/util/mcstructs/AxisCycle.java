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
package com.soulfiremc.server.util.mcstructs;

public enum AxisCycle {
  NONE {
    @Override
    public int cycle(int x, int y, int z, Direction.Axis axis) {
      return axis.choose(x, y, z);
    }

    @Override
    public double cycle(double x, double y, double z, Direction.Axis axis) {
      return axis.choose(x, y, z);
    }

    @Override
    public Direction.Axis cycle(Direction.Axis axis) {
      return axis;
    }

    @Override
    public AxisCycle inverse() {
      return this;
    }
  },
  FORWARD {
    @Override
    public int cycle(int x, int y, int z, Direction.Axis axis) {
      return axis.choose(z, x, y);
    }

    @Override
    public double cycle(double x, double y, double z, Direction.Axis axis) {
      return axis.choose(z, x, y);
    }

    @Override
    public Direction.Axis cycle(Direction.Axis axis) {
      return AXIS_VALUES[Math.floorMod(axis.ordinal() + 1, 3)];
    }

    @Override
    public AxisCycle inverse() {
      return BACKWARD;
    }
  },
  BACKWARD {
    @Override
    public int cycle(int x, int y, int z, Direction.Axis axis) {
      return axis.choose(y, z, x);
    }

    @Override
    public double cycle(double x, double y, double z, Direction.Axis axis) {
      return axis.choose(y, z, x);
    }

    @Override
    public Direction.Axis cycle(Direction.Axis axis) {
      return AXIS_VALUES[Math.floorMod(axis.ordinal() - 1, 3)];
    }

    @Override
    public AxisCycle inverse() {
      return FORWARD;
    }
  };

  public static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
  public static final AxisCycle[] VALUES = values();

  public static AxisCycle between(Direction.Axis to, Direction.Axis axis2) {
    return VALUES[Math.floorMod(axis2.ordinal() - to.ordinal(), 3)];
  }

  public abstract int cycle(int x, int y, int z, Direction.Axis axis);

  public abstract double cycle(double x, double y, double z, Direction.Axis axis);

  public abstract Direction.Axis cycle(Direction.Axis axis);

  public abstract AxisCycle inverse();
}
