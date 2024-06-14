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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.pathfinding.NoRouteFoundException;
import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.pathfinding.graph.ProjectedLevel;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.test.utils.TestBlockAccessor;
import com.soulfiremc.test.utils.TestLevelHeightAccessor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PathfindingTest {
  // TODO: Implement default tagstate for testing
  private static final TagsState TAGS_STATE = new TagsState();

  @Test
  public void testPathfindingStraight() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor), inventory,
      true, true), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());

    assertEquals(2, route.size());
  }

  @Test
  public void testPathfindingImpossible() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder =
      new RouteFinder(
        new MinecraftGraph(TAGS_STATE,
          new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
          inventory,
          true, true),
        // This is impossible to reach
        new PosGoal(3, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    assertThrowsExactly(
      NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
  }

  @Test
  public void testPathfindingDiagonal() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 1, BlockType.STONE);
    accessor.setBlockAt(2, 0, 2, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(2, 1, 2));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());

    assertEquals(2, route.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testPathfindingJump(int height) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, height, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(1, height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void testPathfindingJumpDiagonal(int height) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, height, 1, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(1, height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingFall(int height) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, -height, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(1, -height + 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingFallDiagonal(int height) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, -height, 1, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(1, -height + 1, 1));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (height > 3) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testPathfindingGapJump(int gapLength) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(gapLength + 1, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of());
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(gapLength + 1, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    // TODO: Allow longer jumps
    if (gapLength > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @Test
  public void testPathfindingUp() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.STONE)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(0, 2, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
    assertEquals(1, route.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {15, 20, 25})
  public void testPathfindingUpStacking(int amount) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.STONE).withAmount(amount)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(0, 21, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (amount < 20) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(20, route.size());
    }
  }

  @Test
  public void testPathfindingDown() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
    assertEquals(1, route.size());
  }

  @Test
  public void testPathfindingThroughWallToMoveUp() {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 0, 0, BlockType.STONE);
    accessor.setBlockAt(1, 1, 0, BlockType.STONE);
    accessor.setBlockAt(1, 2, 0, BlockType.STONE);
    accessor.setBlockAt(2, 0, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(2, 1, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
    assertEquals(3, route.size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPathfindingDigSideUnsafe(boolean unsafe) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.STONE);
    if (unsafe) {
      accessor.setBlockAt(1, 0, 0, BlockType.LAVA);
    }

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (unsafe) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  public void testPathfindingDigBelowUnsafe(int level) {
    var accessor = new TestBlockAccessor();
    accessor.setBlockAt(0, 0, 0, BlockType.STONE);
    accessor.setBlockAt(0, -1, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -2, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -3, 0, BlockType.LAVA);
    accessor.setBlockAt(0, -4, 0, BlockType.LAVA);

    accessor.setBlockAt(0, -level, 0, BlockType.STONE);

    var inventory = new ProjectedInventory(List.of(SFItemStack.forTypeSingle(ItemType.DIAMOND_PICKAXE)));
    var routeFinder = new RouteFinder(new MinecraftGraph(TAGS_STATE,
      new ProjectedLevel(TestLevelHeightAccessor.INSTANCE, accessor),
      inventory,
      true, true), new PosGoal(0, 0, 0));

    var initialState = NodeState.forInfo(new SFVec3i(0, 1, 0), inventory);

    if (level > 1) {
      assertThrowsExactly(
        NoRouteFoundException.class, () -> routeFinder.findRoute(initialState, false, new CompletableFuture<>()));
    } else {
      var route = routeFinder.findRoute(initialState, false, new CompletableFuture<>());
      assertEquals(1, route.size());
    }
  }
}
