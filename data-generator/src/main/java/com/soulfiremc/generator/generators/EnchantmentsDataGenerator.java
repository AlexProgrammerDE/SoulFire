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
import java.util.Arrays;
import java.util.Objects;
import lombok.SneakyThrows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentsDataGenerator implements IDataGenerator {
  @SneakyThrows
  public static JsonObject generateEnchantment(Enchantment enchantment) {
    var enchantmentDesc = new JsonObject();

    enchantmentDesc.addProperty("id", BuiltInRegistries.ENCHANTMENT.getId(enchantment));
    enchantmentDesc.addProperty(
      "key",
      Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(enchantment)).toString());

    enchantmentDesc.addProperty("minLevel", enchantment.getMinLevel());
    enchantmentDesc.addProperty("maxLevel", enchantment.getMaxLevel());

    var incompatible = new JsonArray();
    BuiltInRegistries.ENCHANTMENT.stream()
      .filter(other -> other != enchantment)
      .filter(other -> !enchantment.isCompatibleWith(other))
      .forEach(
        other ->
          incompatible.add(
            Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(other)).toString()));

    enchantmentDesc.add("incompatible", incompatible);

    enchantmentDesc.addProperty("supportedItems", enchantment.getSupportedItems().location().toString());

    if (enchantment.isTradeable()) {
      enchantmentDesc.addProperty("tradeable", true);
    }

    if (enchantment.isDiscoverable()) {
      enchantmentDesc.addProperty("discoverable", true);
    }

    if (enchantment.isCurse()) {
      enchantmentDesc.addProperty("curse", true);
    }

    if (enchantment.isTreasureOnly()) {
      enchantmentDesc.addProperty("treasureOnly", true);
    }

    var slotsField = Enchantment.class.getDeclaredField("definition");
    slotsField.setAccessible(true);
    var definition = (Enchantment.EnchantmentDefinition) slotsField.get(enchantment);
    var slotsArray = new JsonArray();
    Arrays.stream(definition.slots()).map(Enum::name).forEach(slotsArray::add);
    enchantmentDesc.add("slots", slotsArray);

    return enchantmentDesc;
  }

  @Override
  public String getDataName() {
    return "data/enchantments.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultsArray = new JsonArray();
    BuiltInRegistries.ENCHANTMENT.stream()
      .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantment)));
    return resultsArray;
  }
}
