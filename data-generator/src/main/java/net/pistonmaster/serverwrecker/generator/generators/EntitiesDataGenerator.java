package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public class EntitiesDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "entities.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultArray = new JsonArray();
        var entityTypeRegistry = BuiltInRegistries.ENTITY_TYPE;
        entityTypeRegistry.forEach(entity -> resultArray.add(generateEntity(entityTypeRegistry, entity)));
        return resultArray;
    }

    public static JsonObject generateEntity(Registry<EntityType<?>> entityRegistry, EntityType<?> entityType) {
        var entityDesc = new JsonObject();
        var registryKey = entityRegistry.getResourceKey(entityType).orElseThrow().location();
        var entityRawId = entityRegistry.getId(entityType);

        entityDesc.addProperty("id", entityRawId);
        entityDesc.addProperty("name", registryKey.getPath());

        entityDesc.addProperty("width", entityType.getDimensions().width);
        entityDesc.addProperty("height", entityType.getDimensions().height);

        entityDesc.addProperty("friendly", entityType.getCategory().isFriendly());

        return entityDesc;
    }
}
