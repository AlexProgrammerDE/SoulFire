package net.pistonmaster.serverwrecker.generator.generators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.pistonmaster.serverwrecker.generator.util.GeneratorConstants;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

import java.util.Locale;

public class BlocksJavaGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "BlockType.java";
    }

    @Override
    public String generateDataJson() {
        var base = ResourceHelper.getResource("/templates/BlockType.java");
        return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                BuiltInRegistries.BLOCK
                        .stream().map(s -> {
                            var name = BuiltInRegistries.BLOCK.getKey(s).getPath();
                            return "public static final BlockType " + name.toUpperCase(Locale.ROOT) + " = register(\"" + name + "\");";
                        })
                        .toArray(String[]::new)));
    }
}
