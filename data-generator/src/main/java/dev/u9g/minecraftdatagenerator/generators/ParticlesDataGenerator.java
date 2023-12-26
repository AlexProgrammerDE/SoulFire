package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ParticlesDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "particles";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<ParticleType<?>>particleTypeRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.PARTICLE_TYPE);
        particleTypeRegistry.forEach(particleType -> resultsArray.add(generateParticleType(particleTypeRegistry, particleType)));
        return resultsArray;
    }

    public static JsonObject generateParticleType(Registry<ParticleType<?>> registry, ParticleType<?> particleType) {
        JsonObject effectDesc = new JsonObject();
        Identifier registryKey = registry.getKey(particleType).orElseThrow().getValue();

        effectDesc.addProperty("id", registry.getRawId(particleType));
        effectDesc.addProperty("name", registryKey.getPath());
        return effectDesc;
    }
}
