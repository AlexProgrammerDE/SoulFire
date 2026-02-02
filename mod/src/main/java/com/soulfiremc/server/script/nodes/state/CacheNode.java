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
package com.soulfiremc.server.script.nodes.state;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/// State node that provides an in-memory key-value cache with optional TTL.
public final class CacheNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("state.cache")
    .displayName("Cache")
    .category(CategoryRegistry.STATE)
    .addInputs(
      PortDefinition.inputWithDefault("operation", "Operation", PortType.STRING, "\"get\"", "Operation: get, set, delete, clear, has"),
      PortDefinition.inputWithDefault("key", "Key", PortType.STRING, "\"\"", "Cache key"),
      PortDefinition.inputWithDefault("value", "Value", PortType.ANY, "null", "Value to store (for set)"),
      PortDefinition.inputWithDefault("ttlMs", "TTL (ms)", PortType.NUMBER, "0", "Time-to-live (0 = no expiry)"),
      PortDefinition.inputWithDefault("namespace", "Namespace", PortType.STRING, "\"default\"", "Cache namespace")
    )
    .addOutputs(
      PortDefinition.output("value", "Value", PortType.ANY, "Retrieved value"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether key was found"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether operation succeeded")
    )
    .description("In-memory key-value cache with optional TTL")
    .icon("hard-drive")
    .color("#0EA5E9")
    .addKeywords("cache", "store", "memory", "ttl", "temporary")
    .build();

  private static final Map<String, Map<String, CacheEntry>> CACHES = new ConcurrentHashMap<>();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var operation = getStringInput(inputs, "operation", "get").toLowerCase();
    var key = getStringInput(inputs, "key", "");
    var value = inputs.get("value");
    var ttlMs = getLongInput(inputs, "ttlMs", 0L);
    var namespace = getStringInput(inputs, "namespace", "default");

    var cache = CACHES.computeIfAbsent(namespace, _ -> new ConcurrentHashMap<>());

    return switch (operation) {
      case "get" -> {
        var entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
          if (entry != null) {
            cache.remove(key);
          }
          yield completed(results(
            "value", NodeValue.ofNull(),
            "found", false,
            "success", true
          ));
        }
        yield completed(results(
          "value", entry.value,
          "found", true,
          "success", true
        ));
      }
      case "set" -> {
        var expiresAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : 0;
        cache.put(key, new CacheEntry(value != null ? value : NodeValue.ofNull(), expiresAt));
        yield completed(results(
          "value", NodeValue.ofNull(),
          "found", false,
          "success", true
        ));
      }
      case "delete" -> {
        var existed = cache.remove(key) != null;
        yield completed(results(
          "value", NodeValue.ofNull(),
          "found", existed,
          "success", true
        ));
      }
      case "clear" -> {
        cache.clear();
        yield completed(results(
          "value", NodeValue.ofNull(),
          "found", false,
          "success", true
        ));
      }
      case "has" -> {
        var entry = cache.get(key);
        var found = entry != null && !entry.isExpired();
        if (entry != null && entry.isExpired()) {
          cache.remove(key);
        }
        yield completed(results(
          "value", NodeValue.ofNull(),
          "found", found,
          "success", true
        ));
      }
      default -> completed(results(
        "value", NodeValue.ofNull(),
        "found", false,
        "success", false
      ));
    };
  }

  private record CacheEntry(NodeValue value, long expiresAt) {
    boolean isExpired() {
      return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }
  }
}
