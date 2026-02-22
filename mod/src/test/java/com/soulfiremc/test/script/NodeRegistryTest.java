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

import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;

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
}
