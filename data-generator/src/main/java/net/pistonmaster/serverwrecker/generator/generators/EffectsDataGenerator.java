package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EffectsDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "effects.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultsArray = new JsonArray();
        var statusEffectRegistry = BuiltInRegistries.MOB_EFFECT;
        statusEffectRegistry.forEach(effect -> resultsArray.add(generateEffect(statusEffectRegistry, effect)));
        return resultsArray;
    }

    public static JsonObject generateEffect(Registry<MobEffect> registry, MobEffect statusEffect) {
        var effectDesc = new JsonObject();
        var registryKey = registry.getResourceKey(statusEffect).orElseThrow().location();

        effectDesc.addProperty("id", registry.getId(statusEffect));
        effectDesc.addProperty("name", Arrays.stream(registryKey.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining()));

        effectDesc.addProperty("type", statusEffect.isBeneficial() ? "good" : "bad");
        return effectDesc;
    }
}
