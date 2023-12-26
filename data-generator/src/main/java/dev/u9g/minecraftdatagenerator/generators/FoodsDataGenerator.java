package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.util.Objects;

public class FoodsDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "foods";
    }

    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<Item> itemRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.ITEM);
        itemRegistry.stream()
                .filter(Item::isEdible)
                .forEach(food -> resultsArray.add(generateFoodDescriptor(itemRegistry, food)));
        return resultsArray;
    }

    public static JsonObject generateFoodDescriptor(Registry<Item> registry, Item foodItem) {
        JsonObject foodDesc = new JsonObject();
        ResourceLocation registryKey = registry.getResourceKey(foodItem).orElseThrow().location();

        foodDesc.addProperty("id", registry.getId(foodItem));
        foodDesc.addProperty("name", registryKey.getPath());

        foodDesc.addProperty("stackSize", foodItem.getMaxStackSize());
        foodDesc.addProperty("displayName", DGU.translateText(foodItem.getDescriptionId()));

        FoodProperties foodComponent = Objects.requireNonNull(foodItem.getFoodProperties());
        float foodPoints = foodComponent.getNutrition();
        float saturationRatio = foodComponent.getSaturationModifier() * 2.0F;
        float saturation = foodPoints * saturationRatio;

        foodDesc.addProperty("foodPoints", foodPoints);
        foodDesc.addProperty("saturation", saturation);

        foodDesc.addProperty("effectiveQuality", foodPoints + saturation);
        foodDesc.addProperty("saturationRatio", saturationRatio);
        return foodDesc;
    }
}
