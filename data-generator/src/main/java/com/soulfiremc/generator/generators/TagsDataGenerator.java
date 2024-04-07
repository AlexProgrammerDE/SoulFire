/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.generator.generators;

import com.soulfiremc.generator.util.GeneratorConstants;
import com.soulfiremc.generator.util.ResourceHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;

@Slf4j
public class TagsDataGenerator {
  private TagsDataGenerator() {}

  public static List<ResourceLocation> generateTag(Class<?> tagClass) {
    var resultArray = new ArrayList<ResourceLocation>();
    for (var field : tagClass.getDeclaredFields()) {
      try {
        var tag = (TagKey<?>) field.get(null);
        resultArray.add(tag.location());
      } catch (IllegalAccessException e) {
        log.error("Failed to generate tag", e);
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
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(BlockTags.class).stream()
            .map(
              s ->
                "public static final ResourceKey "
                  + s.getPath().toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                  + " = register(\""
                  + s
                  + "\");")
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
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(ItemTags.class).stream()
            .map(
              s ->
                "public static final ResourceKey "
                  + s.getPath().toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                  + " = register(\""
                  + s
                  + "\");")
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
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(EntityTypeTags.class).stream()
            .map(
              s ->
                "public static final ResourceKey "
                  + s.getPath().toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                  + " = register(\""
                  + s
                  + "\");")
            .toArray(String[]::new)));
    }
  }

  public static class FluidTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "FluidTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResource("/templates/FluidTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(FluidTags.class).stream()
            .map(
              s ->
                "public static final ResourceKey "
                  + s.getPath().toUpperCase(Locale.ROOT).replace("/", "_WITH_")
                  + " = register(\""
                  + s
                  + "\");")
            .toArray(String[]::new)));
    }
  }
}
