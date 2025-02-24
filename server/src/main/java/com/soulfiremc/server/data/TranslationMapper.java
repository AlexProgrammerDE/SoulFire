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
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.GsonInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgumentLike;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class TranslationMapper implements Function<TranslatableComponent, String> {
  public static final TranslationMapper INSTANCE;

  static {
    var translations = GsonInstance.GSON.fromJson(
      SFHelpers.getResourceAsString("minecraft/en_us.json"), JsonObject.class);
    var mojangTranslations = new Object2ObjectOpenHashMap<String, String>();
    for (var translationEntry : translations.entrySet()) {
      mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
    }

    INSTANCE = new TranslationMapper(mojangTranslations);
  }

  private final Map<String, String> mojangTranslations;

  @Override
  public String apply(TranslatableComponent component) {
    var translation = mojangTranslations.getOrDefault(component.key(), Objects.requireNonNullElse(component.fallback(), component.key()));

    var args =
      component.arguments().stream()
        .map(TranslationArgumentLike::asComponent)
        .map(SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER::serialize)
        .toArray(String[]::new);
    return String.format(translation, (Object[]) args);
  }
}
