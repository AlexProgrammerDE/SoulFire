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
package com.soulfiremc.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.generator.util.MCHelper;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Objects;

public class ItemsJsonGenerator implements IDataGenerator {
  public static JsonObject generateItem(Item item) {
    var itemDesc = new JsonObject();

    itemDesc.addProperty("id", BuiltInRegistries.ITEM.getId(item));
    itemDesc.addProperty("key", BuiltInRegistries.ITEM.getKey(item).toString());

    var sortedComponentObj = new JsonObject();
    item.components().stream().map(typed -> {
        var data = MCHelper.serializeToBase64(registryBuf -> {
          registryBuf.writeVarInt(BuiltInRegistries.DATA_COMPONENT_TYPE.getId(typed.type()));
          writeComponent(registryBuf, typed);
        });

        return Map.entry(
          Objects.requireNonNull(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(typed.type())).toString(),
          data);
      })
      .sorted(Map.Entry.comparingByKey())
      .forEach(e -> sortedComponentObj.add(e.getKey(), new JsonPrimitive(e.getValue())));
    itemDesc.add("components", sortedComponentObj);

    return itemDesc;
  }

  @Override
  public String getDataName() {
    return "data/items.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultArray = new JsonArray();
    BuiltInRegistries.ITEM.forEach(item -> resultArray.add(generateItem(item)));
    return resultArray;
  }

  private static <T> void writeComponent(RegistryFriendlyByteBuf buf, TypedDataComponent<T> typed) {
    typed.type().streamCodec().encode(buf, typed.value());
  }
}
