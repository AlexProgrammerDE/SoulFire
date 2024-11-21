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
package com.soulfiremc.server.protocol.bot.state.registry;

import com.soulfiremc.server.data.Registry;
import com.soulfiremc.server.data.RegistryValue;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;

@Getter
public class Biome implements RegistryValue<Biome> {
  private final Key key;
  private final int id;
  private final Registry<Biome> registry;
  private final float temperature;
  private final float downfall;

  public Biome(Key key, int id, Registry<Biome> registry, NbtMap biomeData) {
    this.key = key;
    this.id = id;
    this.registry = registry;
    this.temperature = biomeData.getFloat("temperature");
    this.downfall = biomeData.getFloat("downfall");
  }
}
