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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

@Slf4j
public class RegistryKeysJavaGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/RegistryKeys.java";
  }

  @Override
  public String generateDataJson() {
    var base = ResourceHelper.getResourceAsString("/templates/RegistryKeys.java");
    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      String.join("\n  ",
        FieldGenerationHelper.mapFields(Registries.class, ResourceKey.class, ResourceKey::location)
          .map(f -> "public static final ResourceKey<?> %s = ResourceKey.key(\"%s\");".formatted(f.name(), f.value()))
          .toArray(String[]::new)));
  }
}
