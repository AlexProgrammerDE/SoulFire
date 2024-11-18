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
package com.soulfiremc.server.protocol.bot.container;

import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.Map;
import java.util.Optional;

public record SFDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents) {
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getOptional(DataComponentType<T> type) {
    // DataComponents can be a null value in this HashMap (even if containsKey() == true)
    // This means it will even remove the explicit vanilla default value for the component key
    var component = dataComponents.get(type);
    return component == null ? Optional.empty() : Optional.of((T) component.getValue());
  }

  public <T> T get(DataComponentType<T> type) {
    return getOptional(type).orElseThrow();
  }
}
