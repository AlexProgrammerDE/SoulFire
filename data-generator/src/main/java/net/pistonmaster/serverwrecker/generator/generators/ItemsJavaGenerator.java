package net.pistonmaster.serverwrecker.generator.generators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.pistonmaster.serverwrecker.generator.util.GeneratorConstants;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

import java.util.Locale;

public class ItemsJavaGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "ItemType.java";
    }

    @Override
    public String generateDataJson() {
        var base = ResourceHelper.getResource("/templates/ItemType.java");
        return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                BuiltInRegistries.ITEM
                        .stream().map(s -> {
                            var name = BuiltInRegistries.ITEM.getKey(s).getPath();
                            return "public static final ItemType " + name.toUpperCase(Locale.ROOT) + " = register(\"" + name + "\");";
                        })
                        .toArray(String[]::new)));
    }
}
