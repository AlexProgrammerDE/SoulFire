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
package com.soulfiremc.test;

import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.pathfinding.NoRouteFoundException;
import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.util.structs.DefaultTagsState;
import com.soulfiremc.test.utils.TestBlockAccessorBuilder;
import com.soulfiremc.test.utils.TestPathConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class PathfindingTest {
  @Test
  public void testPathfindingStraight() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);

    assertEquals(2, route.size());
  }

  @Test
  public void testPathfindingImpossible() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder =
      new RouteFinder(
        new MinecraftGraph(DefaultTagsState.TAGS_STATE,
          accessor.build(),
          inventory,
          TestPathConstraint.INSTANCE),
        // This is impossible to reach
        new PosGoal(3, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    assertThrowsExactly(
      NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
  }

  @Test
  public void testPathfindingDiagonal() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 1, BlockType.STONE);
    accessor.setBlockAt(2, 0, 2, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 2));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);

    assertEquals(2, route.size());
  }

  @Test
  public void testPathfindingDiagonalImpossible() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 1, BlockType.STONE);
    accessor.setBlockAt(2, 0, 2, BlockType.STONE);

    // Barricade
    accessor.setBlockAt(1, 1, 2, BlockType.BEDROCK);
    accessor.setBlockAt(1, 2, 2, BlockType.BEDROCK);
    accessor.setBlockAt(2, 1, 1, BlockType.BEDROCK);
    accessor.setBlockAt(2, 2, 1, BlockType.BEDROCK);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 2));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    assertThrowsExactly(
      NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testPathfindingJump(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, height, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testPathfindingJumpDiagonal(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, height, 1, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingFall(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, -height, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, -height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingFallDiagonal(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, -height, 1, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, -height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingGapJump(int gapLength) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(gapLength + 1, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(gapLength + 1, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    // TODO: Allow longer jumps
    if (gapLength > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @Test
  public void testPathfindingUp() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.STONE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 2, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    assertEquals(1, route.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {15, 20, 25})
  public void testPathfindingUpStacking(int amount) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeWithAmount(ItemType.STONE, amount)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 21, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (amount < 20) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(20, route.size());
    }
  }

  @Test
  public void testPathfindingDown() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    assertEquals(1, route.size());
  }

  @Test
  public void testPathfindingThroughWallToMoveUp() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 1, 0, BlockType.STONE);
    accessor.setBlockAt(1, 2, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    assertEquals(3, route.size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPathfindingMoveUpSideUnsafe(boolean unsafe) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, 3, 0, BlockType.STONE);
    if (unsafe) {
      accessor.setBlockAt(1, 3, 0, BlockType.WATER);
    }

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 2, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (unsafe) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(2, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPathfindingDigSideUnsafe(boolean unsafe) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.STONE);
    if (unsafe) {
      accessor.setBlockAt(1, 0, 0, BlockType.LAVA);
    }

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (unsafe) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  public void testPathfindingDigBelowUnsafe(int level) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -2, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -3, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -4, 0, BlockType.LAVA);

    accessor.setBlockAt(0, -level, 0, BlockType.STONE);

    var inventory = ProjectedInventory.forUnitTest(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)), DefaultTagsState.TAGS_STATE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(DefaultTagsState.TAGS_STATE,
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (level > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      assertEquals(1, route.size());
    }
  }
}
