package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EnchantmentsDataGenerator implements IDataGenerator {

    private static final ImmutableMap<EnchantmentTarget, String> ENCHANTMENT_TARGET_NAMES = ImmutableMap.<EnchantmentTarget, String>builder()
            .put(EnchantmentTarget.ARMOR, "armor")
            .put(EnchantmentTarget.ARMOR_FEET, "armor_feet")
            .put(EnchantmentTarget.ARMOR_LEGS, "armor_legs")
            .put(EnchantmentTarget.ARMOR_CHEST, "armor_chest")
            .put(EnchantmentTarget.ARMOR_HEAD, "armor_head")
            .put(EnchantmentTarget.WEAPON, "weapon")
            .put(EnchantmentTarget.DIGGER, "digger")
            .put(EnchantmentTarget.FISHING_ROD, "fishing_rod")
            .put(EnchantmentTarget.TRIDENT, "trident")
            .put(EnchantmentTarget.BREAKABLE, "breakable")
            .put(EnchantmentTarget.BOW, "bow")
            .put(EnchantmentTarget.WEARABLE, "wearable")
            .put(EnchantmentTarget.CROSSBOW, "crossbow")
            .put(EnchantmentTarget.VANISHABLE, "vanishable")
            .build();

    public static String getEnchantmentTargetName(EnchantmentTarget target) {
        return ENCHANTMENT_TARGET_NAMES.getOrDefault(target, target.name().toLowerCase(Locale.ROOT));
    }

    //Equation enchantment costs follow is a * level + b, so we can easily retrieve a and b by passing zero level
    private static JsonObject generateEnchantmentMinPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMinPower(0);
        int a = enchantment.getMinPower(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    private static JsonObject generateEnchantmentMaxPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMaxPower(0);
        int a = enchantment.getMaxPower(1) - b;

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
        Registry<Enchantment> enchantmentRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        enchantmentRegistry.stream()
                .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantmentRegistry, enchantment)));
        return resultsArray;
    }

    public static JsonObject generateEnchantment(Registry<Enchantment> registry, Enchantment enchantment) {
        JsonObject enchantmentDesc = new JsonObject();
        Identifier registryKey = registry.getKey(enchantment).orElseThrow().getValue();

        enchantmentDesc.addProperty("id", registry.getRawId(enchantment));
        enchantmentDesc.addProperty("name", registryKey.getPath());
        enchantmentDesc.addProperty("displayName", DGU.translateText(enchantment.getTranslationKey()));

        enchantmentDesc.addProperty("maxLevel", enchantment.getMaxLevel());
        enchantmentDesc.add("minCost", generateEnchantmentMinPowerCoefficients(enchantment));
        enchantmentDesc.add("maxCost", generateEnchantmentMaxPowerCoefficients(enchantment));

        enchantmentDesc.addProperty("treasureOnly", enchantment.isTreasure());
        enchantmentDesc.addProperty("curse", enchantment.isCursed());

        List<Enchantment> incompatibleEnchantments = registry.stream()
                .filter(other -> !enchantment.canCombine(other))
                .filter(other -> other != enchantment)
                .collect(Collectors.toList());

        JsonArray excludes = new JsonArray();
        for (Enchantment excludedEnchantment : incompatibleEnchantments) {
            Identifier otherKey = registry.getKey(excludedEnchantment).orElseThrow().getValue();
            excludes.add(otherKey.getPath());
        }
        enchantmentDesc.add("exclude", excludes);

        enchantmentDesc.addProperty("category", getEnchantmentTargetName(enchantment.target));
        enchantmentDesc.addProperty("weight", enchantment.getRarity().getWeight());
        enchantmentDesc.addProperty("tradeable", enchantment.isAvailableForEnchantedBookOffer());
        enchantmentDesc.addProperty("discoverable", enchantment.isAvailableForRandomSelection());

        return enchantmentDesc;
    }
}
