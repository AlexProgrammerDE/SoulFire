package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;

public class EntitiesDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "entities";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultArray = new JsonArray();
        Registry<EntityType<?>> entityTypeRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        entityTypeRegistry.forEach(entity -> resultArray.add(generateEntity(entityTypeRegistry, entity)));
        return resultArray;
    }

    public static JsonObject generateEntity(Registry<EntityType<?>> entityRegistry, EntityType<?> entityType) {
        JsonObject entityDesc = new JsonObject();
        ResourceLocation registryKey = entityRegistry.getResourceKey(entityType).orElseThrow().location();
        int entityRawId = entityRegistry.getId(entityType);

        entityDesc.addProperty("id", entityRawId);
        entityDesc.addProperty("internalId", entityRawId);
        entityDesc.addProperty("name", registryKey.getPath());

        entityDesc.addProperty("displayName", DGU.translateText(entityType.getDescriptionId()));
        entityDesc.addProperty("width", entityType.getDimensions().width);
        entityDesc.addProperty("height", entityType.getDimensions().height);

        String entityTypeString = "UNKNOWN";
        MinecraftServer minecraftServer = DGU.getCurrentlyRunningServer();

        if (minecraftServer != null) {
            Entity entityObject = entityType.create(minecraftServer.overworld());
            entityTypeString = entityObject != null ? getEntityTypeForClass(entityObject.getClass()) : "player";
        }
        entityDesc.addProperty("type", entityTypeString);

        return entityDesc;
    }

    //Honestly, both "type" and "category" fields in the schema and examples do not contain any useful information
    //Since category is optional, I will just leave it out, and for type I will assume general entity classification
    //by the Entity class hierarchy (which has some weirdness too by the way)
    private static String getEntityTypeForClass(Class<? extends Entity> entityClass) {
        //Top-level classifications
        if (WaterAnimal.class.isAssignableFrom(entityClass)) {
            return "water_creature";
        }
        if (Animal.class.isAssignableFrom(entityClass)) {
            return "animal";
        }
        if (Monster.class.isAssignableFrom(entityClass)) {
            return "hostile";
        }
        if (AmbientCreature.class.isAssignableFrom(entityClass)) {
            return "ambient";
        }

        //Second level classifications. PathAwareEntity is not included because it
        //doesn't really make much sense to categorize by it
        if (AgeableMob.class.isAssignableFrom(entityClass)) {
            return "passive";
        }
        if (Mob.class.isAssignableFrom(entityClass)) {
            return "mob";
        }

        //Other classifications only include living entities and projectiles. everything else is categorized as other
        if (LivingEntity.class.isAssignableFrom(entityClass)) {
            return "living";
        }
        if (Projectile.class.isAssignableFrom(entityClass)) {
            return "projectile";
        }
        return "other";
    }
}
