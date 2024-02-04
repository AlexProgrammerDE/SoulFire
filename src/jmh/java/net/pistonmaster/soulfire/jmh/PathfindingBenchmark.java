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
package net.pistonmaster.soulfire.jmh;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.RouteFinder;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.goals.PosGoal;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.soulfire.server.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.soulfire.server.protocol.bot.state.TagsState;
import net.pistonmaster.soulfire.test.utils.TestBlockAccessor;
import net.pistonmaster.soulfire.util.ResourceHelper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

@Slf4j
@State(Scope.Benchmark)
public class PathfindingBenchmark {
    private RouteFinder routeFinder;
    private BotEntityState initialState;

    @Setup
    public void setup() {
        var gson = new Gson();
        var byteArrayInputStream = new ByteArrayInputStream(ResourceHelper.getResourceBytes("/world_data.json.zip"));
        try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             var reader = new InputStreamReader(gzipInputStream)) {
            log.info("Reading world data...");
            var worldData = gson.fromJson(reader, JsonObject.class);
            var definitions = worldData.getAsJsonArray("definitions");
            var blockDefinitions = new String[definitions.size()];
            for (var i = 0; i < definitions.size(); i++) {
                blockDefinitions[i] = definitions.get(i).getAsString();
            }

            var data = gson.fromJson(worldData.getAsJsonArray("data"), int[][][].class);

            log.info("Parsing world data...");

            var accessor = new TestBlockAccessor();
            for (var x = 0; x < data.length; x++) {
                var xArray = data[x];
                for (var y = 0; y < xArray.length; y++) {
                    var yArray = xArray[y];
                    for (var z = 0; z < yArray.length; z++) {
                        accessor.setBlockAt(x, y, z,
                                BlockType.getByName(blockDefinitions[yArray[z]]));
                    }
                }
            }

            log.info("Calculating world data...");

            // Find the first safe block at 0 0
            var safeY = 0;
            for (var y = 0; y < 255; y++) {
                if (accessor.getBlockStateAt(0, y, 0).blockType() == BlockType.AIR) {
                    safeY = y;
                    break;
                }
            }

            routeFinder = new RouteFinder(
                    new MinecraftGraph(new TagsState()),
                    new PosGoal(100, 80, 100)
            );

            initialState = new BotEntityState(
                    new SWVec3i(0, safeY, 0),
                    new ProjectedLevelState(accessor),
                    new ProjectedInventory(new PlayerInventoryContainer())
            );

            log.info("Done loading! Testing...");
        } catch (Exception e) {
            log.error("Failed to load world data!", e);
        }
    }

    @Benchmark
    public void calculatePath() {
        routeFinder.findRoute(initialState, true);
    }
}
