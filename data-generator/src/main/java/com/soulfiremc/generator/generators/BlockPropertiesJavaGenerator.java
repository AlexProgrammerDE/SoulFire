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
import net.minecraft.world.level.block.state.properties.*;

import java.util.Arrays;
import java.util.stream.Stream;

public class BlockPropertiesJavaGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/block/BlockProperties.java";
  }

  @Override
  public String generateDataJson() {
    var base = ResourceHelper.getResourceAsString("/templates/BlockProperties.java");
    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      String.join("\n  ",
        Arrays.stream(BlockStateProperties.class.getDeclaredFields())
          .flatMap(f -> {
            try {
              var name = f.getName();
              f.setAccessible(true);
              var value = f.get(null);

              if (!(value instanceof Property)) {
                return Stream.empty();
              }

              return Stream.of(switch (value) {
                case BooleanProperty booleanProperty -> "public static final BooleanProperty %s = BooleanProperty.create(\"%s\");".formatted(name, booleanProperty.getName());
                case EnumProperty<?> enumProperty -> "public static final EnumProperty %s = EnumProperty.create(\"%s\");".formatted(name, enumProperty.getName());
                case IntegerProperty integerProperty -> "public static final IntegerProperty %s = IntegerProperty.create(\"%s\");".formatted(name, integerProperty.getName());
                case null, default -> throw new RuntimeException("Unknown property type: " + f.getType());
              });
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          })
          .toArray(String[]::new)));
  }
}
