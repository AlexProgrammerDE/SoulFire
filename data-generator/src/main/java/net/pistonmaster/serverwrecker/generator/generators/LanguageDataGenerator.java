package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

public class LanguageDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "en_us.json";
    }

    @Override
    public JsonElement generateDataJson() {
        try {
            return new Gson().fromJson(ResourceHelper.getResource("/assets/minecraft/lang/en_us.json"), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate language file", e);
        }
    }
}
