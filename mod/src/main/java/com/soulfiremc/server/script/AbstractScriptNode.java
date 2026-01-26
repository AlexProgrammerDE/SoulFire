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

import com.google.gson.JsonElement;
import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Abstract base class for script nodes providing common utility methods.
/// All subclasses MUST override getMetadata() to provide complete node metadata.
public abstract class AbstractScriptNode implements ScriptNode {

  /// Helper method to get an input value with type casting.
  /// For non-JSON types like Vec3, extracts from the underlying JsonElement if possible.
  @SuppressWarnings("unchecked")
  protected <T> T getInput(Map<String, NodeValue> inputs, String name, T defaultValue) {
    var value = inputs.get(name);
    if (value == null || value.isNull()) {
      return defaultValue;
    }

    // Special handling for Vec3 from JSON array
    if (defaultValue instanceof Vec3) {
      var list = value.asList();
      if (list.size() >= 3) {
        var x = list.get(0).asDouble(0.0);
        var y = list.get(1).asDouble(0.0);
        var z = list.get(2).asDouble(0.0);
        return (T) new Vec3(x, y, z);
      }
      return defaultValue;
    }

    // For bot values
    if (value instanceof NodeValue.Bot(BotConnection bot1) && defaultValue instanceof BotConnection) {
      return (T) bot1;
    }

    return defaultValue;
  }

  /// Helper method to get a double input value.
  protected double getDoubleInput(Map<String, NodeValue> inputs, String name, double defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return value.asDouble(defaultValue);
  }

  /// Helper method to get an int input value.
  protected int getIntInput(Map<String, NodeValue> inputs, String name, int defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return value.asInt(defaultValue);
  }

  /// Helper method to get a long input value.
  protected long getLongInput(Map<String, NodeValue> inputs, String name, long defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return value.asLong(defaultValue);
  }

  /// Helper method to get a float input value.
  protected float getFloatInput(Map<String, NodeValue> inputs, String name, float defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return (float) value.asDouble(defaultValue);
  }

  /// Helper method to get a boolean input value.
  protected boolean getBooleanInput(Map<String, NodeValue> inputs, String name, boolean defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return value.asBoolean(defaultValue);
  }

  /// Helper method to get a string input value.
  protected String getStringInput(Map<String, NodeValue> inputs, String name, String defaultValue) {
    var value = inputs.get(name);
    if (value == null) {
      return defaultValue;
    }
    return value.asString(defaultValue);
  }

  /// Helper method to get a list input value as NodeValues.
  protected List<NodeValue> getListInput(Map<String, NodeValue> inputs, String name) {
    var value = inputs.get(name);
    if (value == null) {
      return List.of();
    }
    return value.asList();
  }

  /// Helper method to get a list of strings.
  protected List<String> getStringListInput(Map<String, NodeValue> inputs, String name) {
    var value = inputs.get(name);
    if (value == null) {
      return List.of();
    }
    return value.asStringList();
  }

  /// Helper method to get a bot from inputs.
  /// Returns null if the "bot" input is not set.
  ///
  /// @param inputs the node inputs
  /// @return the bot connection, or null if not provided
  @Nullable
  protected BotConnection getBotInput(Map<String, NodeValue> inputs) {
    var value = inputs.get("bot");
    if (value == null) {
      return null;
    }
    return value.asBot();
  }

  /// Helper method to require a bot from inputs.
  /// Nodes should be stateless - bot must be explicitly wired as an input.
  ///
  /// @param inputs the node inputs
  /// @return the bot connection
  /// @throws IllegalStateException if no bot is available
  protected BotConnection requireBot(Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);
    if (bot == null) {
      throw new IllegalStateException("This node requires a 'bot' input, but none was provided. " +
        "Connect a bot output from a trigger node or GetBots/GetBotByName node.");
    }
    return bot;
  }

  /// Helper method to get a raw JsonElement from inputs.
  @Nullable
  protected JsonElement getJsonInput(Map<String, NodeValue> inputs, String name) {
    var value = inputs.get(name);
    if (value == null) {
      return null;
    }
    return value.asJsonElement();
  }

  /// Helper method to create a result map with a single value.
  protected Map<String, NodeValue> result(String key, Object value) {
    var map = new HashMap<String, NodeValue>();
    map.put(key, NodeValue.of(value));
    return map;
  }

  /// Helper method to create a result map with multiple values.
  protected Map<String, NodeValue> results(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Must provide key-value pairs");
    }
    var map = new HashMap<String, NodeValue>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], NodeValue.of(keyValuePairs[i + 1]));
    }
    return map;
  }

  /// Helper method to create an empty result.
  protected Map<String, NodeValue> emptyResult() {
    return Map.of();
  }

  /// Helper method to create a completed future with results.
  protected CompletableFuture<Map<String, NodeValue>> completed(Map<String, NodeValue> results) {
    return CompletableFuture.completedFuture(results);
  }

  /// Helper method to create a completed future with empty results.
  protected CompletableFuture<Map<String, NodeValue>> completedEmpty() {
    return CompletableFuture.completedFuture(Map.of());
  }
}
