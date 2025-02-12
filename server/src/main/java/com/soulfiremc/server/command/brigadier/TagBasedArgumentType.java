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
import com.soulfiremc.server.data.RegistryValue;
import com.soulfiremc.server.data.TagKey;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TagBasedArgumentType<T extends RegistryValue<T>, R extends TagResolvable<T>> implements ArgumentType<R> {
  private static final String TAG_PREFIX = "#";
  private final Function<Key, R> directSupplier;
  private final Function<TagKey<T>, R> tagSupplier;
  private final List<Key> directKeys;
  private final List<TagKey<T>> tagKeys;

  public static boolean isAllowedInUnquotedString(final char c) {
    return c >= '0' && c <= '9'
      || c >= 'A' && c <= 'Z'
      || c >= 'a' && c <= 'z'
      || c == '#' || c == ':'
      || c == '_';
  }

  public static String readUnquotedStringCustom(StringReader stringReader) {
    final var start = stringReader.getCursor();
    while (stringReader.canRead() && isAllowedInUnquotedString(stringReader.peek())) {
      stringReader.skip();
    }

    return stringReader.getString().substring(start, stringReader.getCursor());
  }

  public static String readStringCustom(StringReader stringReader) throws CommandSyntaxException {
    if (!stringReader.canRead()) {
      return "";
    }

    final var next = stringReader.peek();
    if (StringReader.isQuotedStringStart(next)) {
      stringReader.skip();
      return stringReader.readStringUntil(next);
    }

    return readUnquotedStringCustom(stringReader);
  }

  @Override
  public R parse(StringReader stringReader) throws CommandSyntaxException {
    final var start = stringReader.getCursor();
    var string = readStringCustom(stringReader);

    if (string.startsWith(TAG_PREFIX)) {
      string = string.substring(TAG_PREFIX.length());
      for (var key : tagKeys) {
        if (key.key().value().equals(string) || key.key().toString().equals(string)) {
          return tagSupplier.apply(key);
        }
      }
    } else {
      for (var key : directKeys) {
        if (key.value().equals(string) || key.toString().equals(string)) {
          return directSupplier.apply(key);
        }
      }
    }

    stringReader.setCursor(start);
    throw new SimpleCommandExceptionType(new LiteralMessage("Invalid tag value"))
      .createWithContext(stringReader);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
    for (var key : directKeys) {
      var keyPath = key.value();
      if (keyPath.startsWith(remaining)) {
        builder.suggest(keyPath);
      }
    }

    for (var key : tagKeys) {
      var keyPath = "#" + key.key().value();
      if (keyPath.startsWith(remaining)) {
        builder.suggest(keyPath);
      }
    }

    return builder.buildFuture();
  }

  @Override
  public Collection<String> getExamples() {
    return Stream.concat(
        directKeys.stream()
          .limit(2)
          .map(Key::value),
        tagKeys.stream()
          .limit(1)
          .map(TagKey::key)
          .map(Key::value)
          .map(s -> TAG_PREFIX + s))
      .toList();
  }
}
