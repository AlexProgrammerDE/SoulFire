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

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.util.UUIDHelper;

import java.util.function.Predicate;

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
      double value = 0;
      if (reader.canRead() && reader.peek() != ' ') {
        value = reader.readDouble();
      }

      return new DoubleAxisData(true, value);
    }

    return new DoubleAxisData(false, reader.readDouble());
  }

  public static Predicate<Entity> parseEntityMatch(BotConnection bot, String input) {
    var parsedUniqueId = UUIDHelper.tryParseUniqueId(input);
    return entity -> {
      if (parsedUniqueId.isPresent() && entity.uuid().equals(parsedUniqueId.get())) {
        return true;
      }

      var entityProfile = bot.getEntityProfile(entity.uuid());
      if (entityProfile.isEmpty()) {
        return false;
      }

      var gameProfile = entityProfile.get().getProfile();
      return gameProfile != null && gameProfile.getName().equalsIgnoreCase(input);
    };
  }

  public record DoubleAxisData(boolean relative, double value) {
  }
}
