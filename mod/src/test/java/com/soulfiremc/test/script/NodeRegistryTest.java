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
package com.soulfiremc.test.script;

import com.soulfiremc.server.script.*;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link NodeRegistry} registration, caching, and metadata.
final class NodeRegistryTest {

  @Test
  void nodeRegistryContainsBasicNodes() {
    assertTrue(NodeRegistry.isRegistered("math.add"), "math.add should be registered");
    assertTrue(NodeRegistry.isRegistered("math.subtract"), "math.subtract should be registered");
    assertTrue(NodeRegistry.isRegistered("math.multiply"), "math.multiply should be registered");
    assertTrue(NodeRegistry.isRegistered("logic.compare"), "logic.compare should be registered");
    assertTrue(NodeRegistry.isRegistered("logic.and"), "logic.and should be registered");
    assertTrue(NodeRegistry.isRegistered("logic.or"), "logic.or should be registered");
    assertTrue(NodeRegistry.isRegistered("flow.branch"), "flow.branch should be registered");
    assertTrue(NodeRegistry.isRegistered("trigger.on_pre_entity_tick"), "trigger.on_pre_entity_tick should be registered");
    assertTrue(NodeRegistry.isRegistered("trigger.on_join"), "trigger.on_join should be registered");
  }

  @Test
  void nodeRegistryCacheReturnsSameInstance() {
    var node1 = NodeRegistry.create("math.add");
    var node2 = NodeRegistry.create("math.add");

    assertNotNull(node1, "First create() should return non-null");
    assertNotNull(node2, "Second create() should return non-null");
    assertSame(node1, node2, "Cached instances should be the same reference");
  }

  @Test
  void nodeRegistryCreateThrowsForUnknownType() {
    assertThrows(IllegalArgumentException.class, () -> NodeRegistry.create("nonexistent.node"),
      "Unknown node type should throw IllegalArgumentException");
  }

  @Test
  void nodeRegistryGetRegisteredCount() {
    assertTrue(NodeRegistry.getRegisteredCount() > 50, "Should have many registered node types");
  }

  @Test
  void registerRejectsMutableFieldNode() {
    var metadata = NodeMetadata.builder()
      .type("test.mutable_node")
      .displayName("Mutable Node")
      .category(CategoryRegistry.ACTIONS)
      .icon("box")
      .build();

    assertThrows(IllegalStateException.class, () ->
        NodeRegistry.register(metadata, MutableTestNode::new),
      "Should reject node with mutable instance fields");
  }

  @Test
  void registerAcceptsStatelessNode() {
    var metadata = NodeMetadata.builder()
      .type("test.stateless_node")
      .displayName("Stateless Node")
      .category(CategoryRegistry.ACTIONS)
      .icon("box")
      .build();

    assertDoesNotThrow(() -> NodeRegistry.register(metadata, StatelessTestNode::new));
  }

  /// Test node with a mutable instance field (should be rejected).
  static class MutableTestNode implements ScriptNode {
    @SuppressWarnings("unused")
    private int counter = 0;

    @Override
    public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
      return Mono.just(Map.of());
    }
  }

  /// Test node with only static and final fields (should be accepted).
  static class StatelessTestNode implements ScriptNode {
    @SuppressWarnings("unused")
    private static final String TYPE = "test.stateless";
    @SuppressWarnings("unused")
    private final String name = "stateless";

    @Override
    public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
      return Mono.just(Map.of());
    }
  }

  @Test
  void allNodesHaveMetadata() {
    for (var type : NodeRegistry.getRegisteredTypes()) {
      var metadata = NodeRegistry.getMetadata(type);

      assertNotNull(metadata, "Node " + type + " has no metadata");
      assertNotNull(metadata.type(), "Node " + type + " has no type");
      assertNotNull(metadata.displayName(), "Node " + type + " has no display name");
      assertNotNull(metadata.category(), "Node " + type + " has no category");
      assertNotNull(metadata.description(), "Node " + type + " has no description");
      assertEquals(type, metadata.type(), "Node metadata type doesn't match registered type");
    }
  }

  @Test
  void registerRejectsMutableFieldInSuperclass() {
    var metadata = NodeMetadata.builder()
      .type("test.mutable_superclass_node")
      .displayName("Mutable Superclass Node")
      .category(CategoryRegistry.ACTIONS)
      .icon("box")
      .build();

    assertThrows(IllegalStateException.class, () ->
        NodeRegistry.register(metadata, MutableSuperclassTestNode::new),
      "Should reject node with mutable instance field inherited from superclass");
  }

  @Test
  void allNodeOutputKeysMatchMetadata() {
    for (var type : NodeRegistry.getRegisteredTypes()) {
      var metadata = NodeRegistry.getMetadata(type);
      if (metadata.isTrigger()) continue;
      // Skip nodes requiring a bot input
      if (metadata.inputs().stream().anyMatch(p -> p.type() == PortType.BOT && p.required())) continue;
      // Skip nodes requiring exec input (need engine context)
      if (metadata.inputs().stream().anyMatch(p -> p.type() == PortType.EXEC)) continue;

      var node = NodeRegistry.create(type);
      var defaults = NodeRegistry.computeDefaultInputs(metadata);
      try {
        var outputs = node.executeReactive(ScriptTestHelper.TEST_RUNTIME, defaults)
          .block(Duration.ofSeconds(2));
        if (outputs != null) {
          var declaredOutputIds = metadata.outputs().stream()
            .map(PortDefinition::id)
            .collect(Collectors.toSet());
          for (var key : outputs.keySet()) {
            assertTrue(declaredOutputIds.contains(key),
              "Node " + type + " returned undeclared output key '" + key
                + "'. Declared: " + declaredOutputIds);
          }
        }
      } catch (Exception _) {
        // Node may fail without proper runtime context; that's OK for this test
      }
    }
  }

  /// Base class with a mutable field for testing hierarchy walk.
  static class MutableBase implements ScriptNode {
    @SuppressWarnings("unused")
    private int baseCounter = 0;

    @Override
    public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
      return Mono.just(Map.of());
    }
  }

  /// Test node inheriting a mutable field from superclass (should be rejected).
  static class MutableSuperclassTestNode extends MutableBase {}
}
