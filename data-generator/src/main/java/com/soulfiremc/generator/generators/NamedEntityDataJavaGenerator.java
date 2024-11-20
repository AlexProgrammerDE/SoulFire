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
import com.soulfiremc.generator.util.MCHelper;
import com.soulfiremc.generator.util.ResourceHelper;
import lombok.SneakyThrows;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class NamedEntityDataJavaGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "java/NamedEntityData.java";
  }

  @Override
  @SuppressWarnings("unchecked")
  public String generateDataJson() {
    var base = ResourceHelper.getResourceAsString("/templates/NamedEntityData.java");
    return base.replace(
      GeneratorConstants.VALUES_REPLACE,
      String.join("\n  ",
        FieldGenerationHelper.mapFields(EntityType.class, EntityType.class)
          .flatMap(f -> {
            var defaultEntity = MCHelper.createEntity(f.value());
            Class<?> clazz = defaultEntity.getClass();
            Stream<String> fields = Stream.empty();

            do {
              fields = Stream.concat(fields, generateFields(clazz));
            } while ((clazz = clazz.getSuperclass()) != null);

            return fields;
          })
          .distinct()
          .sorted()
          .toArray(String[]::new)));
  }

  @SneakyThrows
  private Stream<String> generateFields(Class<?> clazz) {
    var className = FieldGenerationHelper.toSnakeCase(clazz.getSimpleName());

    List<String> fields = new ArrayList<>();
    for (var field : clazz.getDeclaredFields()) {
      if (!EntityDataAccessor.class.isAssignableFrom(field.getType())) {
        continue;
      }

      var dataName = field.getName();
      var fieldPrefix = "DATA_";
      if (dataName.startsWith(fieldPrefix)) {
        dataName = dataName.substring(fieldPrefix.length());
      }

      var fieldSuffix = "_ID";
      if (dataName.endsWith(fieldSuffix)) {
        dataName = dataName.substring(0, dataName.length() - fieldSuffix.length());
      }

      field.setAccessible(true);
      var dataValue = (EntityDataAccessor<?>) field.get(null);
      fields.add("public static final NamedEntityData %s = register(\"%s\", %d, \"%s\");".formatted(
        className + "__" + dataName,
        dataName.toLowerCase(Locale.ROOT),
        dataValue.id(),
        className.toLowerCase(Locale.ROOT)
      ));
    }

    return fields.stream();
  }
}
