package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.Objects;

public class EffectsDataGenerator implements IDataGenerator {

    public static JsonObject generateEffect(MobEffect statusEffect) {
        var effectDesc = new JsonObject();

        effectDesc.addProperty("id", BuiltInRegistries.MOB_EFFECT.getId(statusEffect));
        effectDesc.addProperty("name", Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(statusEffect)).getPath());

        effectDesc.addProperty("type", statusEffect.isBeneficial() ? "good" : "bad");
        return effectDesc;
    }

    @Override
    public String getDataName() {
        return "effects.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultsArray = new JsonArray();
        BuiltInRegistries.MOB_EFFECT.forEach(effect -> resultsArray.add(generateEffect(effect)));
        return resultsArray;
    }
}
