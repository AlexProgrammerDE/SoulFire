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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector2d;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class DynamicXZArgumentType implements ArgumentType<DynamicXZArgumentType.XZLocationMapper> {
  @Override
  public XZLocationMapper parse(StringReader stringReader) throws CommandSyntaxException {
    var x = ArgumentTypeHelper.readAxis(stringReader);
    ArgumentTypeHelper.mustReadSpace(stringReader);
    var z = ArgumentTypeHelper.readAxis(stringReader);

    return baseLocation -> {
      var xValue = x.relative() ? baseLocation.getX() + x.value() : x.value();
      var zValue = z.relative() ? baseLocation.getY() + z.value() : z.value();

      return Vector2d.from(xValue, zValue);
    };
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("~ ~", "~ ~1", "1 2", "~ ~-1", "~ ~0");
  }

  public interface XZLocationMapper {
    Vector2d getAbsoluteLocation(Vector2d baseLocation);
  }
}
