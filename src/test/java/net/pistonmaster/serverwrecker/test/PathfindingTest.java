/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.test;

import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.server.pathfinding.NoRouteFoundException;
import net.pistonmaster.serverwrecker.server.pathfinding.RouteFinder;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.server.pathfinding.goals.PosGoal;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.serverwrecker.server.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.TagsState;
import net.pistonmaster.serverwrecker.test.utils.TestBlockAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class PathfindingTest {
    private static final ContainerSlot[] EMPTY_CONTAINER = new ContainerSlot[]{};

    @Test
    public void testPathfindingStraight() {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, 0, 0, BlockType.STONE);
        accessor.setBlockAt(2, 0, 0, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(2, 1, 0)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        var route = routeFinder.findRoute(initialState, false);

        assertEquals(2, route.size());
    }

    @Test
    public void testPathfindingImpossible() {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, 0, 0, BlockType.STONE);
        accessor.setBlockAt(2, 0, 0, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                // This is impossible to reach
                new PosGoal(3, 1, 0)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        assertThrowsExactly(NoRouteFoundException.class,
                () -> routeFinder.findRoute(initialState, false));
    }

    @Test
    public void testPathfindingDiagonal() {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, 0, 1, BlockType.STONE);
        accessor.setBlockAt(2, 0, 2, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(2, 1, 2)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        var route = routeFinder.findRoute(initialState, false);

        assertEquals(2, route.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void testPathfindingJump(int height) {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, height, 0, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(1, height + 1, 0)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        if (height > 1) {
            assertThrowsExactly(NoRouteFoundException.class,
                    () -> routeFinder.findRoute(initialState, false));
        } else {
            var route = routeFinder.findRoute(initialState, false);
            assertEquals(1, route.size());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void testPathfindingJumpDiagonal(int height) {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, height, 1, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(1, height + 1, 1)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        if (height > 1) {
            assertThrowsExactly(NoRouteFoundException.class,
                    () -> routeFinder.findRoute(initialState, false));
        } else {
            var route = routeFinder.findRoute(initialState, false);
            assertEquals(1, route.size());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void testPathfindingFall(int height) {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, -height, 0, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(1, -height + 1, 0)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        if (height > 3) {
            assertThrowsExactly(NoRouteFoundException.class,
                    () -> routeFinder.findRoute(initialState, false));
        } else {
            var route = routeFinder.findRoute(initialState, false);
            assertEquals(1, route.size());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void testPathfindingFallDiagonal(int height) {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(1, -height, 1, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(1, -height + 1, 1)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        if (height > 3) {
            assertThrowsExactly(NoRouteFoundException.class,
                    () -> routeFinder.findRoute(initialState, false));
        } else {
            var route = routeFinder.findRoute(initialState, false);
            assertEquals(1, route.size());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void testPathfindingGapJump(int gapLength) {
        var accessor = new TestBlockAccessor();
        accessor.setBlockAt(0, 0, 0, BlockType.STONE);
        accessor.setBlockAt(gapLength + 1, 0, 0, BlockType.STONE);

        var routeFinder = new RouteFinder(
                new MinecraftGraph(new TagsState()),
                new PosGoal(gapLength + 1, 1, 0)
        );

        var initialState = new BotEntityState(
                new SWVec3i(0, 1, 0),
                new ProjectedLevelState(accessor),
                new ProjectedInventory(EMPTY_CONTAINER)
        );

        // TODO: Allow longer jumps
        if (gapLength > 1) {
            assertThrowsExactly(NoRouteFoundException.class,
                    () -> routeFinder.findRoute(initialState, false));
        } else {
            var route = routeFinder.findRoute(initialState, false);
            assertEquals(1, route.size());
        }
    }
}
