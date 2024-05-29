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
import it.unimi.dsi.fastutil.Pair;
import java.util.Arrays;
import java.util.stream.Stream;
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

  public static Stream<Pair<String, ResourceLocation>> generateTag(Class<?> tagClass) {
    return Arrays.stream(tagClass.getDeclaredFields()).map(f -> {
      try {
        return Pair.of(f.getName(), ((TagKey<?>) f.get(null)).location());
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static class BlockTagsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "java/BlockTags.java";
    }

    @Override
    public String generateDataJson() {
      var base = ResourceHelper.getResource("/templates/BlockTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(BlockTags.class)
            .map(
              s ->
                "public static final TagKey<BlockType> %s = register(\"%s\");".formatted(s.left(), s.right().toString()))
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
      var base = ResourceHelper.getResource("/templates/ItemTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(ItemTags.class)
            .map(
              s ->
                "public static final TagKey<ItemType> %s = register(\"%s\");".formatted(s.left(), s.right().toString()))
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
      var base = ResourceHelper.getResource("/templates/EntityTypeTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(EntityTypeTags.class)
            .map(
              s ->
                "public static final TagKey<EntityType> %s = register(\"%s\");".formatted(s.left(), s.right().toString()))
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
      var base = ResourceHelper.getResource("/templates/FluidTags.java");
      return base.replace(
        GeneratorConstants.VALUES_REPLACE,
        String.join(
          "\n  ",
          generateTag(FluidTags.class)
            .map(
              s ->
                "public static final TagKey<FluidType> %s = register(\"%s\");".formatted(s.left(), s.right().toString()))
            .toArray(String[]::new)));
    }
  }
}
