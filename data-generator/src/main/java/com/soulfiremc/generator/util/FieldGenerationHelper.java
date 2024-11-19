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
package com.soulfiremc.generator.util;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public class FieldGenerationHelper {
  public static <F> Stream<FieldNameValuePair<F>> mapFields(Class<?> clazz, Class<F> fieldClass) {
    return mapFields(clazz, fieldClass, Function.identity());
  }

  @SuppressWarnings("unchecked")
  public static <F, C> Stream<FieldNameValuePair<C>> mapFields(Class<?> clazz, Class<F> fieldClass, Function<F, C> mapper) {
    var list = new ArrayList<FieldNameValuePair<C>>();

    for (var field : clazz.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      if (!fieldClass.isAssignableFrom(field.getType())) {
        continue;
      }

      field.setAccessible(true);

      try {
        list.add(new FieldNameValuePair<>(field.getName(), mapper.apply((F) field.get(null))));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    return list.stream();
  }

  public record FieldNameValuePair<T>(String name, T value) {
  }

  public static String toSnakeCase(String camelCase) {
    return camelCase
      .replaceAll("([a-z])([A-Z]+)", "$1_$2")
      .toUpperCase(Locale.ROOT);
  }
}
