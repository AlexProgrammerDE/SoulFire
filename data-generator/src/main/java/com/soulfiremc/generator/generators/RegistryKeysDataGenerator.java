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
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

@Slf4j
public class RegistryKeysDataGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/RegistryKeys.java";
  }

  @Override
  public String generateDataJson() {
    var base = ResourceHelper.getResource("/templates/RegistryKeys.java");
    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      String.join(
        "\n  ",
        Arrays.stream(Registries.class.getDeclaredFields()).map(f -> {
            try {
              return Pair.of(f.getName(), f.get(null));
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException(e);
            }
          })
          .filter(p -> p.right() instanceof ResourceKey<?>)
          .map(
            p -> {
              var value = (ResourceKey<?>) p.right();
              return "public static final ResourceKey<?> %s = ResourceKey.key(\"%s\");".formatted(p.left(), value.location());
            })
          .toArray(String[]::new)));
  }
}
