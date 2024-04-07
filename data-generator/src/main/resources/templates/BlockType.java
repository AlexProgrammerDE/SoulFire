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
package com.soulfiremc.data;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.With;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record BlockType(
  int id,
  ResourceKey key,
  float destroyTime,
  float explosionResistance,
  boolean air,
  boolean fallingBlock,
  boolean replaceable,
  boolean requiresCorrectToolForDrops,
  FluidType fluidType,
  List<LootPoolEntry> lootTableData,
  OffsetData offsetData,
  BlockStates statesData) {
  public static final TypeAdapter<FluidType> CUSTOM_FLUID_TYPE = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, FluidType value) throws IOException {
      out.value(value.key().toString());
    }

    @Override
    public FluidType read(JsonReader in) throws IOException {
      return FluidType.getByKey(ResourceKey.fromString(in.nextString()));
    }
  };
  public static final Int2ReferenceMap<BlockType> FROM_ID = new Int2ReferenceOpenHashMap<>();
  public static final Object2ReferenceMap<ResourceKey, BlockType> FROM_KEY =
    new Object2ReferenceOpenHashMap<>();

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static BlockType register(String key) {
    var instance = GsonDataHelper.fromJson("/minecraft/blocks.json", key, BlockType.class, Map.of(
      FluidType.class,
      CUSTOM_FLUID_TYPE
    ));
    instance =
      instance.withStatesData(
        BlockStates.fromJsonArray(
          instance,
          GsonDataHelper.fromJson("/minecraft/blocks.json", key, JsonObject.class)
            .getAsJsonArray("states")));

    FROM_ID.put(instance.id(), instance);
    FROM_KEY.put(instance.key(), instance);
    return instance;
  }

  public static BlockType getById(int id) {
    return FROM_ID.get(id);
  }

  public static BlockType getByKey(ResourceKey key) {
    return FROM_KEY.get(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BlockType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  public record OffsetData(
    float maxHorizontalOffset, float maxVerticalOffset, OffsetType offsetType) {
    public enum OffsetType {
      XZ,
      XYZ
    }
  }
}
