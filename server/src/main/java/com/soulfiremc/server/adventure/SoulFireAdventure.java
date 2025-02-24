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
package com.soulfiremc.server.adventure;

import com.soulfiremc.server.data.TranslationMapper;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;

public class SoulFireAdventure {
  public static final ComponentFlattener FLATTENER =
    ComponentFlattener.basic().toBuilder()
      .mapper(TranslatableComponent.class, TranslationMapper.INSTANCE)
      .build();
  public static final PlainTextComponentSerializer PLAIN_MESSAGE_SERIALIZER =
    PlainTextComponentSerializer.builder()
      .flattener(FLATTENER)
      .build();
  public static final ANSIComponentSerializer ANSI_SERIALIZER =
    ANSIComponentSerializer.builder()
      .flattener(FLATTENER)
      .build();
  public static final ANSIComponentSerializer TRUE_COLOR_ANSI_SERIALIZER =
    ANSIComponentSerializer.builder()
      .flattener(FLATTENER)
      .colorLevel(ColorLevel.TRUE_COLOR)
      .build();
}
