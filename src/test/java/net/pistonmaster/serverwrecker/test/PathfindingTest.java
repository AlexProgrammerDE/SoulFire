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
import net.pistonmaster.serverwrecker.server.pathfinding.RouteFinder;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.server.pathfinding.goals.PosGoal;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.serverwrecker.server.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.TagsState;
import net.pistonmaster.serverwrecker.test.utils.TestBlockAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathfindingTest {
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
                new ProjectedInventory(new PlayerInventoryContainer())
        );

        var route = routeFinder.findRoute(initialState, true);

        assertEquals(3, route.size());
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
                new ProjectedInventory(new PlayerInventoryContainer())
        );

        var route = routeFinder.findRoute(initialState, true);

        assertEquals(3, route.size());
    }
}
