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
package net.pistonmaster.soulfire.server.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgumentLike;
import net.pistonmaster.soulfire.server.SoulFireServer;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class TranslationMapper implements Function<TranslatableComponent, String> {
    private final Map<String, String> mojangTranslations;

    @Override
    public String apply(TranslatableComponent component) {
        var translation = mojangTranslations.get(component.key());

        if (translation == null) {
            log.warn("Missing translation for key: {}", component.key());
            return component.key();
        }

        var args = component.arguments().stream()
                .map(TranslationArgumentLike::asComponent)
                .map(SoulFireServer.PLAIN_MESSAGE_SERIALIZER::serialize)
                .toArray(String[]::new);
        return String.format(translation, (Object[]) args);
    }
}
