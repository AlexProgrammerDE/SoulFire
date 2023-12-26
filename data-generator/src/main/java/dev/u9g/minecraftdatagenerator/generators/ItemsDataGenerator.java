package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsDataGenerator implements IDataGenerator {

    private static List<Item> calculateItemsToRepairWith(Registry<Item> itemRegistry, Item sourceItem) {
        ItemStack sourceItemStack = sourceItem.getDefaultInstance();
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
        return "items";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultArray = new JsonArray();
        Registry<Item> itemRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.ITEM);
        itemRegistry.stream().forEach(item -> resultArray.add(generateItem(itemRegistry, item)));
        return resultArray;
    }

    public static JsonObject generateItem(Registry<Item> itemRegistry, Item item) {
        JsonObject itemDesc = new JsonObject();
        ResourceLocation registryKey = itemRegistry.getResourceKey(item).orElseThrow().location();

        itemDesc.addProperty("id", itemRegistry.getId(item));
        itemDesc.addProperty("name", registryKey.getPath());

        itemDesc.addProperty("displayName", DGU.translateText(item.getDescriptionId()));
        itemDesc.addProperty("stackSize", item.getMaxStackSize());

        List<EnchantmentCategory> enchantmentTargets = getApplicableEnchantmentTargets(item);

        JsonArray enchantCategoriesArray = new JsonArray();
        for (EnchantmentCategory target : enchantmentTargets) {
            enchantCategoriesArray.add(EnchantmentsDataGenerator.getEnchantmentTargetName(target));
        }
        if (!enchantCategoriesArray.isEmpty()) {
            itemDesc.add("enchantCategories", enchantCategoriesArray);
        }

        if (item.canBeDepleted()) {
            List<Item> repairWithItems = calculateItemsToRepairWith(itemRegistry, item);

            JsonArray fixedWithArray = new JsonArray();
            for (Item repairWithItem : repairWithItems) {
                ResourceLocation repairWithName = itemRegistry.getResourceKey(repairWithItem).orElseThrow().location();
                fixedWithArray.add(repairWithName.getPath());
            }
            if (fixedWithArray.size() > 0) {
                itemDesc.add("repairWith", fixedWithArray);
            }

            int maxDurability = item.getMaxDamage();
            itemDesc.addProperty("maxDurability", maxDurability);
        }
        return itemDesc;
    }
}
