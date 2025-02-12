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
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {
  private final Class<T> type;

  @Override
  public T parse(StringReader stringReader) throws CommandSyntaxException {
    final var start = stringReader.getCursor();
    var string = stringReader.readString();

    for (var constant : type.getEnumConstants()) {
      if (constant.name().equalsIgnoreCase(string)) {
        return constant;
      }
    }

    stringReader.setCursor(start);
    throw new SimpleCommandExceptionType(new LiteralMessage("Invalid enum value"))
      .createWithContext(stringReader);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

    for (var constant : type.getEnumConstants()) {
      if (constant.name().toLowerCase(Locale.ROOT).startsWith(remaining)) {
        builder.suggest(constant.name());
      }
    }

    return builder.buildFuture();
  }

  @Override
  public Collection<String> getExamples() {
    return Stream.of(type.getEnumConstants())
      .map(Enum::name)
      .toList();
  }
}
