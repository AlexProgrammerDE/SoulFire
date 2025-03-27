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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.Registry;
import com.soulfiremc.server.data.RegistryKeys;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.protocol.bot.state.registry.Biome;
import com.soulfiremc.server.protocol.bot.state.registry.DimensionType;
import com.soulfiremc.server.protocol.bot.state.registry.SFChatType;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SFRegistriesState {
  private final Map<ResourceKey<?>, List<RegistryEntry>> resolvedRegistryData = new LinkedHashMap<>();
  private final Registry<DimensionType> dimensionTypeRegistry = new Registry<>(RegistryKeys.DIMENSION_TYPE);
  private final Registry<Biome> biomeRegistry = new Registry<>(RegistryKeys.BIOME);
  private final Registry<SFChatType> chatTypeRegistry = new Registry<>(RegistryKeys.CHAT_TYPE);
}
