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
import lombok.SneakyThrows;
import net.minecraft.world.level.material.MapColor;

public class MapColorJavaGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/MapColor.java";
  }

  @SneakyThrows
  @Override
  public String generateDataJson() {
    var base = ResourceHelper.getResourceAsString("/templates/MapColor.java");
    var registryField = MapColor.class.getDeclaredField("MATERIAL_COLORS");
    registryField.setAccessible(true);
    var registry = (MapColor[]) registryField.get(null);
    var colField = MapColor.class.getDeclaredField("col");
    colField.setAccessible(true);
    var colorArray = new String[registry.length];
    for (var i = 0; i < registry.length; i++) {
      if (registry[i] == null) {
        colorArray[i] = "null";
        continue;
      }

      colorArray[i] = "new MapColor(%d, %s)".formatted(i, colField.get(registry[i]));
    }

    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      "public static final MapColor[] COLORS = new MapColor[] {\n    %s\n  };".formatted(String.join(",\n    ", colorArray)));
  }
}
