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

import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.test.utils.TestBlockAccessorBuilder;
import com.soulfiremc.test.utils.TestBootstrap;
import com.soulfiremc.test.utils.TestMiningCostCalculator;
import com.soulfiremc.test.utils.TestPathConstraint;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class PathfindingTest {
  @BeforeAll
  static void setup() {
    // Bootstrap mixins and Minecraft registries
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void pathfindingStraight() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 0, 0, Blocks.STONE);
    accessor.setBlockAt(2, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);

    var foundRouteResult = assertInstanceOf(
      RouteFinder.FoundRouteResult.class, route);
    assertEquals(2, foundRouteResult.actions().size());
  }

  @Test
  void pathfindingImpossible() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 0, 0, Blocks.STONE);
    accessor.setBlockAt(2, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder =
      new RouteFinder(
        new MinecraftGraph(
          accessor.build(),
          inventory,
          TestPathConstraint.INSTANCE),
        // This is impossible to reach
        new PosGoal(3, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    assertInstanceOf(
      RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
  }

  @Test
  void pathfindingDiagonal() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 0, 1, Blocks.STONE);
    accessor.setBlockAt(2, 0, 2, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 2));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);

    var foundRouteResult = assertInstanceOf(
      RouteFinder.FoundRouteResult.class, route);
    assertEquals(2, foundRouteResult.actions().size());
  }

  @Test
  void pathfindingDiagonalImpossible() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 0, 1, Blocks.STONE);
    accessor.setBlockAt(2, 0, 2, Blocks.STONE);

    // Barricade
    accessor.setBlockAt(1, 1, 2, Blocks.BEDROCK);
    accessor.setBlockAt(1, 2, 2, Blocks.BEDROCK);
    accessor.setBlockAt(2, 1, 1, Blocks.BEDROCK);
    accessor.setBlockAt(2, 2, 1, Blocks.BEDROCK);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 2));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    assertInstanceOf(
      RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void pathfindingJump(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, height, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void pathfindingJumpDiagonal(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, height, 1, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void pathfindingFall(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, -height, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, -height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void pathfindingFallDiagonal(int height) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, -height, 1, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(1, -height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void pathfindingGapJump(int gapLength) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(gapLength + 1, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(gapLength + 1, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    // TODO: Allow longer jumps
    if (gapLength > 1) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @Test
  void pathfindingUp() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.STONE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 2, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    var foundRouteResult = assertInstanceOf(
      RouteFinder.FoundRouteResult.class, route);
    assertEquals(1, foundRouteResult.actions().size());
  }

  @ParameterizedTest
  @ValueSource(ints = {15, 20, 25})
  void pathfindingUpStacking(int amount) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.STONE, amount)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 21, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (amount < 20) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(20, foundRouteResult.actions().size());
    }
  }

  @Test
  void pathfindingDown() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(0, -1, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.DIAMOND_PICKAXE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    var foundRouteResult = assertInstanceOf(
      RouteFinder.FoundRouteResult.class, route);
    assertEquals(1, foundRouteResult.actions().size());
  }

  @Test
  void pathfindingThroughWallToMoveUp() {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 0, 0, Blocks.STONE);
    accessor.setBlockAt(1, 1, 0, Blocks.STONE);
    accessor.setBlockAt(1, 2, 0, Blocks.STONE);
    accessor.setBlockAt(2, 0, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.DIAMOND_PICKAXE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRouteSync(initialState);
    var foundRouteResult = assertInstanceOf(
      RouteFinder.FoundRouteResult.class, route);
    assertEquals(3, foundRouteResult.actions().size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void pathfindingMoveUpSideUnsafe(boolean unsafe) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(0, 3, 0, Blocks.STONE);
    if (unsafe) {
      accessor.setBlockAt(1, 3, 0, Blocks.WATER);
    }

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.DIAMOND_PICKAXE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 2, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (unsafe) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(2, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void pathfindingDigSideUnsafe(boolean unsafe) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(0, -1, 0, Blocks.STONE);
    if (unsafe) {
      accessor.setBlockAt(1, 0, 0, Blocks.LAVA);
    }

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.DIAMOND_PICKAXE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (unsafe) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  void pathfindingDigBelowUnsafe(int level) {
    var accessor = new TestBlockAccessorBuilder();
    accessor.setBlockAt(0, 0, 0, Blocks.STONE);
    accessor.setBlockAt(0, -1, 0, Blocks.LAVA);
    accessor.setBlockAt(0, -2, 0, Blocks.LAVA);
    accessor.setBlockAt(0, -3, 0, Blocks.LAVA);
    accessor.setBlockAt(0, -4, 0, Blocks.LAVA);

    accessor.setBlockAt(0, -level, 0, Blocks.STONE);

    var inventory = new ProjectedInventory(List.of(new ItemStack(Items.DIAMOND_PICKAXE)), TestMiningCostCalculator.INSTANCE, TestPathConstraint.INSTANCE);
    var routeFinder = new RouteFinder(new MinecraftGraph(
      accessor.build(),
      inventory,
      TestPathConstraint.INSTANCE), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (level > 1) {
      assertInstanceOf(
        RouteFinder.NoRouteFoundResult.class, routeFinder.findRouteSync(initialState));
    } else {
      var route = routeFinder.findRouteSync(initialState);
      var foundRouteResult = assertInstanceOf(
        RouteFinder.FoundRouteResult.class, route);
      assertEquals(1, foundRouteResult.actions().size());
    }
  }
}
