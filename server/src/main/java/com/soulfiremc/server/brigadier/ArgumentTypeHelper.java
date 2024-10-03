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
package com.soulfiremc.server.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class ArgumentTypeHelper {
  private ArgumentTypeHelper() {
  }

  public static void mustReadSpace(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || reader.peek() != ' ') {
      throw new SimpleCommandExceptionType(new LiteralMessage("Expected space")).createWithContext(reader);
    }

    reader.skip();
  }

  public static DoubleAxisData readAxis(StringReader reader) throws CommandSyntaxException {
    if (reader.canRead() && reader.peek() == '~') {
      reader.skip();
      return new DoubleAxisData(true, reader.readDouble());
    }

    return new DoubleAxisData(false, reader.readDouble());
  }

  public record DoubleAxisData(boolean relative, double value) {
  }
}
