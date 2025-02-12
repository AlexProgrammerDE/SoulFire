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

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class DynamicYArgumentType implements ArgumentType<DynamicYArgumentType.YLocationMapper> {
  @Override
  public YLocationMapper parse(StringReader stringReader) throws CommandSyntaxException {
    var y = ArgumentTypeHelper.readAxis(stringReader);

    return baseLocation -> y.relative() ? baseLocation + y.value() : y.value();
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("~", "~1", "1", "~-1", "~0");
  }

  public interface YLocationMapper {
    double getAbsoluteLocation(double baseLocation);
  }
}
