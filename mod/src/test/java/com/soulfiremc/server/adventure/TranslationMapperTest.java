/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.adventure;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TranslationMapperTest {
  static Stream<String> formats() {
    return Stream.of("%s", "%2$s/%s/%%", "%%s", "%%%s", "%1$s%1$s", "%01$s", "",
      "%3$s", "%", "%b", "%n", "%S", "%d", "%<s", "%20s", "%2147483648$s", "%0$s", "%1$%", "%s %", "%2$s %s %s");
  }

  @ParameterizedTest
  @MethodSource("formats")
  void matchesMinecraftFormattingAndMalformedFallbacks(String format) {
    var key = UUID.randomUUID().toString();
    var first = UUID.randomUUID().toString();
    var second = UUID.randomUUID().toString();
    var nativeComponent = net.minecraft.network.chat.Component.translatableWithFallback(key, format, first, second);
    var component = Component.translatable(key).fallback(format).arguments(Component.text(first), Component.text(second));

    assertEquals(nativeComponent.getString(), new TranslationMapper(Map.of()).apply(component));
  }
}
