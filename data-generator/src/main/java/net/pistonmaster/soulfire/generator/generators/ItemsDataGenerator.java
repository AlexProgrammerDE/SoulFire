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
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemsDataGenerator implements IDataGenerator {

    private static List<Item> calculateItemsToRepairWith(Item sourceItem) {
        var sourceItemStack = sourceItem.getDefaultInstance();
        return BuiltInRegistries.ITEM.stream()
                .filter(otherItem -> sourceItem.isValidRepairItem(sourceItemStack, otherItem.getDefaultInstance()))
                .collect(Collectors.toList());
    }

    private static List<EnchantmentCategory> getApplicableEnchantmentTargets(Item sourceItem) {
        return Arrays.stream(EnchantmentCategory.values())
                .filter(target -> target.canEnchant(sourceItem))
                .collect(Collectors.toList());
    }

    public static JsonObject generateItem(Item item) {
        var itemDesc = new JsonObject();

        itemDesc.addProperty("id", BuiltInRegistries.ITEM.getId(item));
        itemDesc.addProperty("name", BuiltInRegistries.ITEM.getKey(item).getPath());

        itemDesc.addProperty("maxStackSize", item.getMaxStackSize());

        var enchantmentTargets = getApplicableEnchantmentTargets(item);

        var enchantCategoriesArray = new JsonArray();
        for (var target : enchantmentTargets) {
            enchantCategoriesArray.add(EnchantmentsDataGenerator.getEnchantmentTargetName(target));
        }

        if (!enchantCategoriesArray.isEmpty()) {
            itemDesc.add("enchantCategories", enchantCategoriesArray);
        }

        if (item.canBeDepleted()) {
            var depletionData = new JsonObject();
            var repairWithItems = calculateItemsToRepairWith(item);

            var fixedWithArray = new JsonArray();
            for (var repairWithItem : repairWithItems) {
                fixedWithArray.add(BuiltInRegistries.ITEM.getKey(repairWithItem).getPath());
            }

            if (!fixedWithArray.isEmpty()) {
                depletionData.add("repairWith", fixedWithArray);
            }

            depletionData.addProperty("maxDamage", item.getMaxDamage());

            itemDesc.add("depletionData", depletionData);
        }

        if (item.isEdible()) {
            var foodComponent = Objects.requireNonNull(item.getFoodProperties());
            var foodDesc = new JsonObject();
            foodDesc.addProperty("nutrition", foodComponent.getNutrition());
            foodDesc.addProperty("saturationModifier", foodComponent.getSaturationModifier());

            if (foodComponent.isFastFood()) {
                foodDesc.addProperty("fastFood", true);
            }

            if (foodComponent.isMeat()) {
                foodDesc.addProperty("isMeat", true);
            }

            if (foodComponent.canAlwaysEat()) {
                foodDesc.addProperty("canAlwaysEat", true);
            }

            if (foodComponent.getEffects().stream()
                    .map(Pair::getFirst)
                    .map(MobEffectInstance::getEffect)
                    .map(MobEffect::getCategory)
                    .anyMatch(c -> c == MobEffectCategory.HARMFUL)) {
                foodDesc.addProperty("possiblyHarmful", true);
            }

            itemDesc.add("foodProperties", foodDesc);
        }

        return itemDesc;
    }

    @Override
    public String getDataName() {
        return "items.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultArray = new JsonArray();
        BuiltInRegistries.ITEM.forEach(item -> resultArray.add(generateItem(item)));
        return resultArray;
    }
}
