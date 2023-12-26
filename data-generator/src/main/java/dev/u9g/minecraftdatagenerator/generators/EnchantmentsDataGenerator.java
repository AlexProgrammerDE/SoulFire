package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EnchantmentsDataGenerator implements IDataGenerator {

    private static final ImmutableMap<EnchantmentCategory, String> ENCHANTMENT_TARGET_NAMES = ImmutableMap.<EnchantmentCategory, String>builder()
            .put(EnchantmentCategory.ARMOR, "armor")
            .put(EnchantmentCategory.ARMOR_FEET, "armor_feet")
            .put(EnchantmentCategory.ARMOR_LEGS, "armor_legs")
            .put(EnchantmentCategory.ARMOR_CHEST, "armor_chest")
            .put(EnchantmentCategory.ARMOR_HEAD, "armor_head")
            .put(EnchantmentCategory.WEAPON, "weapon")
            .put(EnchantmentCategory.DIGGER, "digger")
            .put(EnchantmentCategory.FISHING_ROD, "fishing_rod")
            .put(EnchantmentCategory.TRIDENT, "trident")
            .put(EnchantmentCategory.BREAKABLE, "breakable")
            .put(EnchantmentCategory.BOW, "bow")
            .put(EnchantmentCategory.WEARABLE, "wearable")
            .put(EnchantmentCategory.CROSSBOW, "crossbow")
            .put(EnchantmentCategory.VANISHABLE, "vanishable")
            .build();

    public static String getEnchantmentTargetName(EnchantmentCategory target) {
        return ENCHANTMENT_TARGET_NAMES.getOrDefault(target, target.name().toLowerCase(Locale.ROOT));
    }

    //Equation enchantment costs follow is a * level + b, so we can easily retrieve a and b by passing zero level
    private static JsonObject generateEnchantmentMinPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMinCost(0);
        int a = enchantment.getMinCost(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    private static JsonObject generateEnchantmentMaxPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMaxCost(0);
        int a = enchantment.getMaxCost(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    @Override
    public String getDataName() {
        return "enchantments";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<Enchantment> enchantmentRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        enchantmentRegistry.stream()
                .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantmentRegistry, enchantment)));
        return resultsArray;
    }

    public static JsonObject generateEnchantment(Registry<Enchantment> registry, Enchantment enchantment) {
        JsonObject enchantmentDesc = new JsonObject();
        ResourceLocation registryKey = registry.getResourceKey(enchantment).orElseThrow().location();

        enchantmentDesc.addProperty("id", registry.getId(enchantment));
        enchantmentDesc.addProperty("name", registryKey.getPath());
        enchantmentDesc.addProperty("displayName", DGU.translateText(enchantment.getDescriptionId()));

        enchantmentDesc.addProperty("maxLevel", enchantment.getMaxLevel());
        enchantmentDesc.add("minCost", generateEnchantmentMinPowerCoefficients(enchantment));
        enchantmentDesc.add("maxCost", generateEnchantmentMaxPowerCoefficients(enchantment));

        enchantmentDesc.addProperty("treasureOnly", enchantment.isTreasureOnly());
        enchantmentDesc.addProperty("curse", enchantment.isCurse());

        List<Enchantment> incompatibleEnchantments = registry.stream()
                .filter(other -> !enchantment.isCompatibleWith(other))
                .filter(other -> other != enchantment)
                .collect(Collectors.toList());

        JsonArray excludes = new JsonArray();
        for (Enchantment excludedEnchantment : incompatibleEnchantments) {
            ResourceLocation otherKey = registry.getResourceKey(excludedEnchantment).orElseThrow().location();
            excludes.add(otherKey.getPath());
        }
        enchantmentDesc.add("exclude", excludes);

        enchantmentDesc.addProperty("category", getEnchantmentTargetName(enchantment.category));
        enchantmentDesc.addProperty("weight", enchantment.getRarity().getWeight());
        enchantmentDesc.addProperty("tradeable", enchantment.isTradeable());
        enchantmentDesc.addProperty("discoverable", enchantment.isDiscoverable());

        return enchantmentDesc;
    }
}
