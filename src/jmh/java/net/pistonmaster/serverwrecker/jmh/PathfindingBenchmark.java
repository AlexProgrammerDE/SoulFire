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
package net.pistonmaster.serverwrecker.jmh;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.data.ResourceData;
import net.pistonmaster.serverwrecker.util.ResourceHelper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

@State(Scope.Benchmark)
public class PathfindingBenchmark {
    @Setup
    public void setup() {
        // Load all the data
        if (ResourceData.GLOBAL_BLOCK_PALETTE.toString().isEmpty()) {
            throw new IllegalStateException("Something went horribly wrong!");
        }

        var gson = new Gson();
        var byteArrayInputStream = new ByteArrayInputStream(ResourceHelper.getResourceBytes("/world_data.json.zip"));
        try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             var reader = new InputStreamReader(gzipInputStream)) {
            System.out.println("Reading world data...");
            var worldData = gson.fromJson(reader, JsonObject.class);
            var definitions = worldData.getAsJsonArray("definitions");
            var blockDefinitions = new String[definitions.size()];
            for (var i = 0; i < definitions.size(); i++) {
                blockDefinitions[i] = definitions.get(i).getAsString();
            }

            var data = gson.fromJson(worldData.getAsJsonArray("data"), int[][][].class);

            System.out.println("Parsing world data...");

            var yHeight = data[0][0].length;
            var blocks = new BlockType[data.length][][];
            for (var x = 0; x < data.length; x++) {
                var xArray = data[x];
                var xBlocksArray = blocks[x] = new BlockType[xArray.length][];
                for (var y = 0; y < xArray.length; y++) {
                    var yArray = xArray[y];
                    var yBlocksArray = xBlocksArray[y] = new BlockType[yArray.length];
                    for (var z = 0; z < yArray.length; z++) {
                        yBlocksArray[z] = BlockType.getByName(blockDefinitions[yArray[z]]);
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    public void calculatePath(Blackhole bh) {

    }
}
