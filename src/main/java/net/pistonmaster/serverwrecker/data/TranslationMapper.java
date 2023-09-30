/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.data;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class TranslationMapper implements Function<TranslatableComponent, String> {
    private final Map<String, String> mojangTranslations;
    private final Logger logger = LoggerFactory.getLogger(TranslationMapper.class);
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    @Override
    public String apply(TranslatableComponent component) {
        String translation = mojangTranslations.get(component.key());

        if (translation == null) {
            logger.warn("Missing translation for key: " + component.key());
            return component.key();
        }

        String[] args = new String[component.args().size()];

        for (int i = 0; i < component.args().size(); i++) {
            args[i] = plainSerializer.serialize(component.args().get(i));
        }

        return String.format(translation, (Object[]) args);
    }
}
