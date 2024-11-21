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
import org.jetbrains.annotations.Nullable;

@Getter
public class DimensionType implements RegistryValue<DimensionType> {
  private final Key key;
  private final int id;
  private final Registry<DimensionType> registry;
  private final String infiniburn;
  private final String effects;
  private final boolean ultrawarm;
  private final int height;
  private final int logicalHeight;
  private final boolean natural;
  private final int minY;
  private final boolean bedWorks;
  private final @Nullable Long fixedTime; // Only nether and end
  private final double coordinateScale;
  private final boolean piglinSafe;
  private final boolean hasCeiling;
  private final boolean hasSkylight;
  private final float ambientLight;
  private final int monsterSpawnBlockLightLimit;
  private final boolean hasRaids;
  private final boolean respawnAnchorWorks;

  public DimensionType(Key key, int id, Registry<DimensionType> registry, NbtMap dimensionTypeData) {
    this.key = key;
    this.id = id;
    this.registry = registry;
    this.infiniburn = dimensionTypeData.getString("infiniburn");
    this.effects = dimensionTypeData.getString("effects");
    this.ultrawarm = dimensionTypeData.getBoolean("ultrawarm");
    this.height = dimensionTypeData.getInt("height");
    this.logicalHeight = dimensionTypeData.getInt("logical_height");
    this.natural = dimensionTypeData.getBoolean("natural");
    this.minY = dimensionTypeData.getInt("min_y");
    this.bedWorks = dimensionTypeData.getBoolean("bed_works");
    this.fixedTime = dimensionTypeData.containsKey("fixed_time") ? dimensionTypeData.getLong("fixed_time") : null;
    this.coordinateScale = dimensionTypeData.getDouble("coordinate_scale");
    this.piglinSafe = dimensionTypeData.getBoolean("piglin_safe");
    this.hasCeiling = dimensionTypeData.getBoolean("has_ceiling");
    this.hasSkylight = dimensionTypeData.getBoolean("has_skylight");
    this.ambientLight = dimensionTypeData.getFloat("ambient_light");
    this.monsterSpawnBlockLightLimit =
      dimensionTypeData.getInt("monster_spawn_block_light_limit");
    this.hasRaids = dimensionTypeData.getBoolean("has_raids");
    this.respawnAnchorWorks = dimensionTypeData.getBoolean("respawn_anchor_works");
  }
}
