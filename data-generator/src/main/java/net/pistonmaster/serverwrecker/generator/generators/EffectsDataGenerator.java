package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.pistonmaster.serverwrecker.generator.util.DGU;
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
        var statusEffectRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        statusEffectRegistry.forEach(effect -> resultsArray.add(generateEffect(statusEffectRegistry, effect)));
        return resultsArray;
    }

    public static JsonObject generateEffect(Registry<MobEffect> registry, MobEffect statusEffect) {
        var effectDesc = new JsonObject();
        var registryKey = registry.getResourceKey(statusEffect).orElseThrow().location();

        effectDesc.addProperty("id", registry.getId(statusEffect));
        effectDesc.addProperty("name", Arrays.stream(registryKey.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining()));
        effectDesc.addProperty("displayName", DGU.translateText(statusEffect.getDescriptionId()));

        effectDesc.addProperty("type", statusEffect.isBeneficial() ? "good" : "bad");
        return effectDesc;
    }
}
