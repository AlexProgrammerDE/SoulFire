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
package com.soulfiremc.server.command.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Collection;
import java.util.List;

public final class DoubleAxisArgumentType implements ArgumentType<DoubleAxisArgumentType.DoubleAxisData> {
  public static final DoubleAxisArgumentType INSTANCE = new DoubleAxisArgumentType();

  private DoubleAxisArgumentType() {}

  public static DoubleAxisData getDoubleAxisData(CommandContext<?> c, String argument) {
    return c.getArgument(argument, DoubleAxisData.class);
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("~", "~1", "1", "~-1", "~0");
  }

  public static double forYAxis(DoubleAxisData yData, double baseValue) {
    return yData.relative() ? baseValue + yData.value() : yData.value();
  }

  public static Vector2d forXZAxis(DoubleAxisData xData, DoubleAxisData zData, Vector2d baseLocation) {
    var xValue = xData.relative() ? baseLocation.getX() + xData.value() : xData.value();
    var zValue = zData.relative() ? baseLocation.getY() + zData.value() : zData.value();

    return Vector2d.from(xValue, zValue);
  }

  public static Vector3d forXYZAxis(DoubleAxisData xData, DoubleAxisData yData, DoubleAxisData zData, Vector3d baseLocation) {
    var xValue = xData.relative() ? baseLocation.getX() + xData.value() : xData.value();
    var yValue = yData.relative() ? baseLocation.getY() + yData.value() : yData.value();
    var zValue = zData.relative() ? baseLocation.getZ() + zData.value() : zData.value();

    return Vector3d.from(xValue, yValue, zValue);
  }

  @Override
  public DoubleAxisData parse(StringReader stringReader) throws CommandSyntaxException {
    if (stringReader.canRead() && stringReader.peek() == '~') {
      stringReader.skip();
      double value = 0;
      if (stringReader.canRead() && stringReader.peek() != ' ') {
        value = stringReader.readDouble();
      }

      return new DoubleAxisData(true, value);
    }

    return new DoubleAxisData(false, stringReader.readDouble());
  }

  public record DoubleAxisData(boolean relative, double value) {
  }
}
