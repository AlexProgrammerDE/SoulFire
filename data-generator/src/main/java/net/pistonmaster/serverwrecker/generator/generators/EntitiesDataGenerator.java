package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public class EntitiesDataGenerator implements IDataGenerator {

    public static JsonObject generateEntity(EntityType<?> entityType) {
        var entityDesc = new JsonObject();

        entityDesc.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType));
        entityDesc.addProperty("name", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath());

        var dimensions = entityType.getDimensions();
        entityDesc.addProperty("width", dimensions.width);
        entityDesc.addProperty("height", dimensions.height);

        var category = entityType.getCategory();
        entityDesc.addProperty("category", category.getName());
        entityDesc.addProperty("friendly", category.isFriendly());

        return entityDesc;
    }

    @Override
    public String getDataName() {
        return "entities.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultArray = new JsonArray();
        BuiltInRegistries.ENTITY_TYPE.forEach(entity -> resultArray.add(generateEntity(entity)));
        return resultArray;
    }
}
