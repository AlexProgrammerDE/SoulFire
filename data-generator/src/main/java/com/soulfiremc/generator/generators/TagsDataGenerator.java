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

import com.soulfiremc.generator.util.FieldGenerationHelper;
import com.soulfiremc.generator.util.GeneratorConstants;
import com.soulfiremc.generator.util.ResourceHelper;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.tags.*;

@Slf4j
public class TagsDataGenerator {
  private TagsDataGenerator() {}

  public static class BlockTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "java/BlockTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResourceAsString("/templates/BlockTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join("\n  ",
          FieldGenerationHelper.mapFields(BlockTags.class, TagKey.class, TagKey::location)
            .map(f -> "public static final TagKey<BlockType> %s = register(\"%s\");".formatted(f.name(), f.value()))
            .toArray(String[]::new)));
    }
  }

  public static class ItemTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "java/ItemTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResourceAsString("/templates/ItemTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join("\n  ",
          FieldGenerationHelper.mapFields(ItemTags.class, TagKey.class, TagKey::location)
            .map(f -> "public static final TagKey<ItemType> %s = register(\"%s\");".formatted(f.name(), f.value()))
            .toArray(String[]::new)));
    }
  }

  public static class EntityTypeTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "java/EntityTypeTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResourceAsString("/templates/EntityTypeTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join("\n  ",
          FieldGenerationHelper.mapFields(EntityTypeTags.class, TagKey.class, TagKey::location)
            .map(f -> "public static final TagKey<EntityType> %s = register(\"%s\");".formatted(f.name(), f.value()))
            .toArray(String[]::new)));
    }
  }

  public static class FluidTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "java/FluidTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResourceAsString("/templates/FluidTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join("\n  ",
          FieldGenerationHelper.mapFields(FluidTags.class, TagKey.class, TagKey::location)
            .map(f -> "public static final TagKey<FluidType> %s = register(\"%s\");".formatted(f.name(), f.value()))
            .toArray(String[]::new)));
    }
  }
}
