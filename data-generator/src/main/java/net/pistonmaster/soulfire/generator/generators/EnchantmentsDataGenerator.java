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
package net.pistonmaster.soulfire.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.Locale;
import java.util.Objects;

public class EnchantmentsDataGenerator implements IDataGenerator {
    public static String getEnchantmentTargetName(EnchantmentCategory target) {
        return target.name().toLowerCase(Locale.ROOT);
    }

    //Equation enchantment costs follow is a * level + b, so we can easily retrieve a and b by passing zero level
    private static JsonObject generateEnchantmentMinPowerCoefficients(Enchantment enchantment) {
        var b = enchantment.getMinCost(0);
        var a = enchantment.getMinCost(1) - b;

        var resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    private static JsonObject generateEnchantmentMaxPowerCoefficients(Enchantment enchantment) {
        var b = enchantment.getMaxCost(0);
        var a = enchantment.getMaxCost(1) - b;

        var resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    public static JsonObject generateEnchantment(Enchantment enchantment) {
        var enchantmentDesc = new JsonObject();

        enchantmentDesc.addProperty("id", BuiltInRegistries.ENCHANTMENT.getId(enchantment));
        enchantmentDesc.addProperty("name", Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(enchantment)).getPath());

        enchantmentDesc.addProperty("maxLevel", enchantment.getMaxLevel());
        enchantmentDesc.add("minCost", generateEnchantmentMinPowerCoefficients(enchantment));
        enchantmentDesc.add("maxCost", generateEnchantmentMaxPowerCoefficients(enchantment));

        enchantmentDesc.addProperty("treasureOnly", enchantment.isTreasureOnly());
        enchantmentDesc.addProperty("curse", enchantment.isCurse());

        var incompatibleEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .filter(other -> !enchantment.isCompatibleWith(other))
                .filter(other -> other != enchantment)
                .toList();

        var excludes = new JsonArray();
        for (var excludedEnchantment : incompatibleEnchantments) {
            excludes.add(Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(excludedEnchantment)).getPath());
        }
        enchantmentDesc.add("exclude", excludes);

        enchantmentDesc.addProperty("category", getEnchantmentTargetName(enchantment.category));
        enchantmentDesc.addProperty("weight", enchantment.getRarity().getWeight());
        enchantmentDesc.addProperty("tradeable", enchantment.isTradeable());
        enchantmentDesc.addProperty("discoverable", enchantment.isDiscoverable());

        return enchantmentDesc;
    }

    @Override
    public String getDataName() {
        return "enchantments.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultsArray = new JsonArray();
        BuiltInRegistries.ENCHANTMENT.stream()
                .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantment)));
        return resultsArray;
    }
}
