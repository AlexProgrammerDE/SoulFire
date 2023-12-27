package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsDataGenerator implements IDataGenerator {

    private static List<Item> calculateItemsToRepairWith(Registry<Item> itemRegistry, Item sourceItem) {
        var sourceItemStack = sourceItem.getDefaultInstance();
        return itemRegistry.stream()
                .filter(otherItem -> sourceItem.isValidRepairItem(sourceItemStack, otherItem.getDefaultInstance()))
                .collect(Collectors.toList());
    }

    private static List<EnchantmentCategory> getApplicableEnchantmentTargets(Item sourceItem) {
        return Arrays.stream(EnchantmentCategory.values())
                .filter(target -> target.canEnchant(sourceItem))
                .collect(Collectors.toList());
    }

    @Override
    public String getDataName() {
        return "items.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultArray = new JsonArray();
        var itemRegistry = BuiltInRegistries.ITEM;
        itemRegistry.forEach(item -> resultArray.add(generateItem(itemRegistry, item)));
        return resultArray;
    }

    public static JsonObject generateItem(Registry<Item> itemRegistry, Item item) {
        var itemDesc = new JsonObject();
        var registryKey = itemRegistry.getResourceKey(item).orElseThrow().location();

        itemDesc.addProperty("id", itemRegistry.getId(item));
        itemDesc.addProperty("name", registryKey.getPath());

        itemDesc.addProperty("stackSize", item.getMaxStackSize());

        var enchantmentTargets = getApplicableEnchantmentTargets(item);

        var enchantCategoriesArray = new JsonArray();
        for (var target : enchantmentTargets) {
            enchantCategoriesArray.add(EnchantmentsDataGenerator.getEnchantmentTargetName(target));
        }

        if (!enchantCategoriesArray.isEmpty()) {
            itemDesc.add("enchantCategories", enchantCategoriesArray);
        }

        if (item.canBeDepleted()) {
            var repairWithItems = calculateItemsToRepairWith(itemRegistry, item);

            var fixedWithArray = new JsonArray();
            for (var repairWithItem : repairWithItems) {
                var repairWithName = itemRegistry.getResourceKey(repairWithItem).orElseThrow().location();
                fixedWithArray.add(repairWithName.getPath());
            }
            if (!fixedWithArray.isEmpty()) {
                itemDesc.add("repairWith", fixedWithArray);
            }

            var maxDurability = item.getMaxDamage();
            itemDesc.addProperty("maxDurability", maxDurability);
        }

        return itemDesc;
    }
}
