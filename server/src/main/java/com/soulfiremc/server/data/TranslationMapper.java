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
package com.soulfiremc.server.data;

import com.google.gson.JsonObject;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.util.GsonInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgumentLike;

@Slf4j
@RequiredArgsConstructor
public class TranslationMapper implements Function<TranslatableComponent, String> {
  public static final TranslationMapper INSTANCE;

  static {
    JsonObject translations;
    try (var stream =
           TranslationMapper.class.getClassLoader().getResourceAsStream("minecraft/en_us.json")) {
      Objects.requireNonNull(stream, "en_us.json not found");
      translations = GsonInstance.GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    var mojangTranslations = new Object2ObjectOpenHashMap<String, String>();
    for (var translationEntry : translations.entrySet()) {
      mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
    }

    INSTANCE = new TranslationMapper(mojangTranslations);
  }

  private final Map<String, String> mojangTranslations;

  @Override
  public String apply(TranslatableComponent component) {
    var translation = mojangTranslations.get(component.key());

    if (translation == null) {
      log.warn("Missing translation for key: {}", component.key());
      return component.key();
    }

    var args =
      component.arguments().stream()
        .map(TranslationArgumentLike::asComponent)
        .map(SoulFireServer.PLAIN_MESSAGE_SERIALIZER::serialize)
        .toArray(String[]::new);
    return String.format(translation, (Object[]) args);
  }
}
