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
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import com.soulfiremc.util.GsonInstance;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.IntDataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.ObjectDataComponent;

@Slf4j
public record JsonDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> components) {
  public static final TypeAdapter<JsonDataComponents> SERIALIZER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, JsonDataComponents value) {
      throw new UnsupportedOperationException("JsonDataComponents serialization is not supported");
    }

    @Override
    public JsonDataComponents read(JsonReader in) {
      var parsedJson = GsonInstance.GSON.<JsonObject>fromJson(in, JsonObject.class);
      var map = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
      for (var entry : parsedJson.entrySet()) {
        var value = entry.getValue();
        switch (entry.getKey()) {
          case "minecraft:max_stack_size" -> map.put(DataComponentType.MAX_STACK_SIZE, new IntDataComponent(DataComponentType.MAX_STACK_SIZE, value.getAsInt()));
          case "minecraft:rarity" -> map.put(DataComponentType.RARITY, new IntDataComponent(DataComponentType.RARITY, Rarity.valueOf(value.getAsString().toUpperCase(Locale.ROOT)).ordinal()));
          case "minecraft:attribute_modifiers" -> map.put(DataComponentType.ATTRIBUTE_MODIFIERS, new ObjectDataComponent<>(DataComponentType.ATTRIBUTE_MODIFIERS, JsonToMCPLCodecs.ITEM_ATTRIBUTE_MODIFIERS_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
          case "minecraft:tool" -> map.put(DataComponentType.TOOL, new ObjectDataComponent<>(DataComponentType.TOOL, JsonToMCPLCodecs.TOOL_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
          case "minecraft:food" -> map.put(DataComponentType.FOOD, new ObjectDataComponent<>(DataComponentType.FOOD, JsonToMCPLCodecs.FOOD_PROPERTIES_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
          default -> log.trace("Unknown DataComponentType: {}", entry.getKey());
        }
      }

      return new JsonDataComponents(map);
    }
  };
}
