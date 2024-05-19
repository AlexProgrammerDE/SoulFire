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
import com.mojang.serialization.JsonOps;
import com.soulfiremc.generator.util.MCHelper;
import java.util.Map;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ItemsDataGenerator implements IDataGenerator {
  public static JsonObject generateItem(Item item) {
    var itemDesc = new JsonObject();

    itemDesc.addProperty("id", BuiltInRegistries.ITEM.getId(item));
    itemDesc.addProperty("key", BuiltInRegistries.ITEM.getKey(item).toString());

    var sortedComponentObj = new JsonObject();
    DataComponentMap.CODEC.encodeStart(
        MCHelper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE),
        item.components()).result().orElseThrow().getAsJsonObject()
      .entrySet().stream().sorted(Map.Entry.comparingByKey())
      .forEach(e -> sortedComponentObj.add(e.getKey(), e.getValue()));
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
}
