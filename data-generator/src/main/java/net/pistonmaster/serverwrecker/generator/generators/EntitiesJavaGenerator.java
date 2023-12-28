package net.pistonmaster.serverwrecker.generator.generators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.pistonmaster.serverwrecker.generator.util.GeneratorConstants;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

import java.util.Locale;

public class EntitiesJavaGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "EntityType.java";
    }

    @Override
    public String generateDataJson() {
        var base = ResourceHelper.getResource("/templates/EntityType.java");
        return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                BuiltInRegistries.ENTITY_TYPE
                        .stream().map(s -> {
                            var name = BuiltInRegistries.ENTITY_TYPE.getKey(s).getPath();
                            return "public static final EntityType " + name.toUpperCase(Locale.ROOT) + " = register(\"" + name + "\");";
                        })
                        .toArray(String[]::new)));
    }
}
