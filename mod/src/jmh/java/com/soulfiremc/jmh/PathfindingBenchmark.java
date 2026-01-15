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
package com.soulfiremc.jmh;

import com.google.gson.JsonObject;
import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.pathfinding.graph.constraint.DelegatePathConstraint;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraint;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.soulfiremc.test.utils.TestBlockAccessorBuilder;
import com.soulfiremc.test.utils.TestBootstrap;
import com.soulfiremc.test.utils.TestMiningCostCalculator;
import com.soulfiremc.test.utils.TestPathConstraint;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NonNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@State(Scope.Benchmark)
public class PathfindingBenchmark {
  private RouteFinder routeFinder;
  private NodeState initialState;

  @Setup
  public void setup() {
    // Bootstrap mixins and Minecraft registries
    TestBootstrap.bootstrapForTest();

    var byteArrayInputStream =
      new ByteArrayInputStream(SFHelpers.getResourceAsBytes("world_data.json.zip"));
    try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
         var reader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)) {
      log.info("Reading world data...");
      var worldData = GsonInstance.GSON.fromJson(reader, JsonObject.class);
      var definitions = worldData.getAsJsonArray("definitions");
      var blockDefinitions = new Key[definitions.size()];
      for (var i = 0; i < definitions.size(); i++) {
        @Subst("minecraft:air") var asString = definitions.get(i).getAsString();
        blockDefinitions[i] = Key.key(asString);
      }

      var data = GsonInstance.GSON.fromJson(worldData.getAsJsonArray("data"), int[][][].class);

      log.info("Parsing world data...");

      log.info("X: {}, Y: {}, Z: {}", data.length, data[0].length, data[0][0].length);

      // Find the first safe block at 0 0
      var safeY = Integer.MIN_VALUE;
      var accessor = new TestBlockAccessorBuilder();
      for (var x = 0; x < data.length; x++) {
        for (var y = 0; y < data[0].length; y++) {
          for (var z = 0; z < data[0][0].length; z++) {
            var key = blockDefinitions[data[x][y][z]];
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(key.asString()));
            if (block.defaultBlockState().isAir()) {
              continue;
            }

            // Insert blocks
            accessor.setBlockAt(x, y, z, block);
            if (x == 0 && z == 0) {
              safeY = Math.max(safeY, y + 1);
            }
          }
        }
      }

      var builtAccessor = accessor.build();

      var pathConstraint = new DelegatePathConstraint() {
        @Override
        @NonNull
        public PathConstraint delegate() {
          return TestPathConstraint.INSTANCE;
        }
      };
      var inventory = new ProjectedInventory(List.of(), TestMiningCostCalculator.INSTANCE, pathConstraint);
      initialState = NodeState.forInfo(new SFVec3i(0, safeY, 0), inventory);
      log.info("Initial state: {}", initialState.blockPosition().formatXYZ());

      routeFinder = new RouteFinder(new MinecraftGraph(
        builtAccessor,
        inventory,
        pathConstraint), new PosGoal(100, 80, 100));

      log.info("Done loading! Testing...");
    } catch (Exception e) {
      log.error("Failed to load world data!", e);
    }
  }

  @Benchmark
  public void calculatePath() {
    routeFinder.findRouteFuture(initialState).join();
  }
}
