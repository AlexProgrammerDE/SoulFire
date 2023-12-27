package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.pistonmaster.serverwrecker.generator.util.DGU;

import java.util.Objects;

public class FoodsDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "foods.json";
    }

    public JsonArray generateDataJson() {
        var resultsArray = new JsonArray();
        var itemRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.ITEM);
        itemRegistry.stream()
                .filter(Item::isEdible)
                .forEach(food -> resultsArray.add(generateFoodDescriptor(itemRegistry, food)));
        return resultsArray;
    }

    public static JsonObject generateFoodDescriptor(Registry<Item> registry, Item foodItem) {
        var foodDesc = new JsonObject();
        var registryKey = registry.getResourceKey(foodItem).orElseThrow().location();

        foodDesc.addProperty("id", registry.getId(foodItem));
        foodDesc.addProperty("name", registryKey.getPath());

        foodDesc.addProperty("stackSize", foodItem.getMaxStackSize());
        foodDesc.addProperty("displayName", DGU.translateText(foodItem.getDescriptionId()));

        var foodComponent = Objects.requireNonNull(foodItem.getFoodProperties());
        float foodPoints = foodComponent.getNutrition();
        var saturationRatio = foodComponent.getSaturationModifier() * 2.0F;
        var saturation = foodPoints * saturationRatio;

        foodDesc.addProperty("foodPoints", foodPoints);
        foodDesc.addProperty("saturation", saturation);

        foodDesc.addProperty("effectiveQuality", foodPoints + saturation);
        foodDesc.addProperty("saturationRatio", saturationRatio);
        return foodDesc;
    }
}
