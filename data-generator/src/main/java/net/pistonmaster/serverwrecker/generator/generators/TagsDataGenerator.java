package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;

public class TagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "tags";
    }

    @Override
    public JsonObject generateDataJson() {
        JsonObject resultObject = new JsonObject();
        resultObject.add("block", generateTag(BlockTags.class));
        resultObject.add("item", generateTag(ItemTags.class));
        resultObject.add("entity_type", generateTag(EntityTypeTags.class));

        return resultObject;
    }

    public static JsonArray generateTag(Class<?> tagClass) {
        JsonArray resultArray = new JsonArray();
        for (var field : tagClass.getDeclaredFields()) {
            try {
                var tag = (TagKey<?>)field.get(null);
                resultArray.add(tag.location().getPath());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return resultArray;
    }
}
