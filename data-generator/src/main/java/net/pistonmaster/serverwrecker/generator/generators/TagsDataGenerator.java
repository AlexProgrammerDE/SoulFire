package net.pistonmaster.serverwrecker.generator.generators;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.pistonmaster.serverwrecker.generator.Main;
import net.pistonmaster.serverwrecker.generator.util.GeneratorConstants;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TagsDataGenerator {
    public static List<String> generateTag(Class<?> tagClass) {
        var resultArray = new ArrayList<String>();
        for (var field : tagClass.getDeclaredFields()) {
            try {
                var tag = (TagKey<?>) field.get(null);
                resultArray.add(tag.location().getPath());
            } catch (IllegalAccessException e) {
                Main.LOGGER.error("Failed to generate tag", e);
            }
        }
        return resultArray;
    }

    public static class BlockTagsDataGenerator implements IDataGenerator {
        @Override
        public String getDataName() {
            return "BlockTags.java";
        }

        @Override
        public String generateDataJson() {
            var base = ResourceHelper.getResource("/templates/BlockTags.java");
            return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                    generateTag(BlockTags.class)
                            .stream().map(s -> "public static final String " + s.toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                                    + " = \"minecraft:" + s + "\";")
                            .toArray(String[]::new)));
        }
    }

    public static class ItemTagsDataGenerator implements IDataGenerator {
        @Override
        public String getDataName() {
            return "ItemTags.java";
        }

        @Override
        public String generateDataJson() {
            var base = ResourceHelper.getResource("/templates/ItemTags.java");
            return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                    generateTag(ItemTags.class)
                            .stream().map(s -> "public static final String " + s.toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                                    + " = \"minecraft:" + s + "\";")
                            .toArray(String[]::new)));
        }
    }

    public static class EntityTypeTagsDataGenerator implements IDataGenerator {
        @Override
        public String getDataName() {
            return "EntityTypeTags.java";
        }

        @Override
        public String generateDataJson() {
            var base = ResourceHelper.getResource("/templates/EntityTypeTags.java");
            return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                    generateTag(EntityTypeTags.class)
                            .stream().map(s -> "public static final String " + s.toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                                    + " = \"minecraft:" + s + "\";")
                            .toArray(String[]::new)));
        }
    }
}
