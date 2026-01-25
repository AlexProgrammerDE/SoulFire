/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Abstract base class for script nodes providing common utility methods.
public abstract class AbstractScriptNode implements ScriptNode {

  /// Helper method to get an input value with type casting.
  @SuppressWarnings("unchecked")
  protected <T> T getInput(Map<String, Object> inputs, String name, T defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    try {
      return (T) value;
    } catch (ClassCastException e) {
      return defaultValue;
    }
  }

  /// Helper method to get a double input value.
  protected double getDoubleInput(Map<String, Object> inputs, String name, double defaultValue) {
    var value = inputs.get(name);
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    return defaultValue;
  }

  /// Helper method to get an int input value.
  protected int getIntInput(Map<String, Object> inputs, String name, int defaultValue) {
    var value = inputs.get(name);
    if (value instanceof Number n) {
      return n.intValue();
    }
    return defaultValue;
  }

  /// Helper method to get a long input value.
  protected long getLongInput(Map<String, Object> inputs, String name, long defaultValue) {
    var value = inputs.get(name);
    if (value instanceof Number n) {
      return n.longValue();
    }
    return defaultValue;
  }

  /// Helper method to get a float input value.
  protected float getFloatInput(Map<String, Object> inputs, String name, float defaultValue) {
    var value = inputs.get(name);
    if (value instanceof Number n) {
      return n.floatValue();
    }
    return defaultValue;
  }

  /// Helper method to get a boolean input value.
  protected boolean getBooleanInput(Map<String, Object> inputs, String name, boolean defaultValue) {
    var value = inputs.get(name);
    if (value instanceof Boolean b) {
      return b;
    }
    return defaultValue;
  }

  /// Helper method to get a string input value.
  protected String getStringInput(Map<String, Object> inputs, String name, String defaultValue) {
    var value = inputs.get(name);
    if (value instanceof String s) {
      return s;
    }
    if (value != null) {
      return value.toString();
    }
    return defaultValue;
  }

  /// Helper method to get a list input value.
  @SuppressWarnings("unchecked")
  protected <T> List<T> getListInput(Map<String, Object> inputs, String name, List<T> defaultValue) {
    var value = inputs.get(name);
    if (value instanceof List<?> list) {
      return (List<T>) list;
    }
    return defaultValue;
  }

  /// Helper method to create a result map with a single value.
  protected Map<String, Object> result(String key, Object value) {
    var map = new HashMap<String, Object>();
    map.put(key, value);
    return map;
  }

  /// Helper method to create a result map with multiple values.
  protected Map<String, Object> results(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Must provide key-value pairs");
    }
    var map = new HashMap<String, Object>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

  /// Helper method to create an empty result.
  protected Map<String, Object> emptyResult() {
    return Map.of();
  }

  /// Helper method to create a completed future with results.
  protected CompletableFuture<Map<String, Object>> completed(Map<String, Object> results) {
    return CompletableFuture.completedFuture(results);
  }

  /// Helper method to create a completed future with empty results.
  protected CompletableFuture<Map<String, Object>> completedEmpty() {
    return CompletableFuture.completedFuture(Map.of());
  }
}
