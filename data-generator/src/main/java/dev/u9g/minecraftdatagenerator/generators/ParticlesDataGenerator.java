package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class ParticlesDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "particles";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<ParticleType<?>>particleTypeRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.PARTICLE_TYPE);
        particleTypeRegistry.forEach(particleType -> resultsArray.add(generateParticleType(particleTypeRegistry, particleType)));
        return resultsArray;
    }

    public static JsonObject generateParticleType(Registry<ParticleType<?>> registry, ParticleType<?> particleType) {
        JsonObject effectDesc = new JsonObject();
        ResourceLocation registryKey = registry.getResourceKey(particleType).orElseThrow().location();

        effectDesc.addProperty("id", registry.getId(particleType));
        effectDesc.addProperty("name", registryKey.getPath());
        return effectDesc;
    }
}
