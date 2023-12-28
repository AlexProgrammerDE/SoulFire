package net.pistonmaster.serverwrecker.generator.generators;

import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

public class LanguageDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "en_us.json";
    }

    @Override
    public String generateDataJson() {
        return ResourceHelper.getResource("/assets/minecraft/lang/en_us.json");
    }
}
