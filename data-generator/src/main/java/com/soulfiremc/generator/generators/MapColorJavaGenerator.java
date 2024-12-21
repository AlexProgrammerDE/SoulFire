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
import net.lenni0451.reflect.stream.RStream;
import net.minecraft.world.level.material.MapColor;

public class MapColorJavaGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/MapColor.java";
  }

  @Override
  public String generateDataJson() {
    var registry = RStream.of(MapColor.class).fields().by("MATERIAL_COLORS").<MapColor[]>get();
    var colorArray = new String[registry.length];
    for (var i = 0; i < registry.length; i++) {
      var mapColor = registry[i];
      if (mapColor == null) {
        colorArray[i] = "null";
        continue;
      }

      var col = RStream.of(mapColor).fields().by("col").<Integer>get();
      colorArray[i] = "new MapColor(%d, %s)".formatted(i, col);
    }

    var base = ResourceHelper.getResourceAsString("/templates/MapColor.java");
    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      "public static final MapColor[] COLORS = new MapColor[] {\n    %s\n  };".formatted(String.join(",\n    ", colorArray)));
  }
}
