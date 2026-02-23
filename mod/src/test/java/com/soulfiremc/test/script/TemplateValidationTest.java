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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.soulfiremc.server.script.ScriptGraph;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// Parameterized test validating that all template JSON files build valid ScriptGraphs.
final class TemplateValidationTest {

  private static final Gson GSON = new Gson();

  static Stream<String> templateFiles() {
    return Stream.of(
      "simple-print.json",
      "data-edges.json",
      "loop.json",
      "branch.json"
    );
  }

  @ParameterizedTest(name = "template: {0}")
  @MethodSource("templateFiles")
  void templateBuildsWithoutErrors(String filename) throws IOException {
    var template = loadTemplate(filename);

    var builder = ScriptGraph.builder("template-" + filename, filename);

    var nodesArray = template.getAsJsonArray("nodes");
    assertNotNull(nodesArray, "Template " + filename + " must have a 'nodes' array");
    assertTrue(nodesArray.size() > 0, "Template " + filename + " must have at least one node");

    for (var element : nodesArray) {
      var node = element.getAsJsonObject();
      var id = node.get("id").getAsString();
      var type = node.get("type").getAsString();

      assertTrue(NodeRegistry.isRegistered(type),
        "Node type '" + type + "' in template " + filename + " is not registered");

      HashMap<String, Object> defaults = null;
      if (node.has("defaultInputs")) {
        defaults = new HashMap<>();
        for (var entry : node.getAsJsonObject("defaultInputs").entrySet()) {
          var val = entry.getValue();
          if (val.isJsonPrimitive()) {
            var prim = val.getAsJsonPrimitive();
            if (prim.isNumber()) {
              defaults.put(entry.getKey(), prim.getAsNumber());
            } else if (prim.isBoolean()) {
              defaults.put(entry.getKey(), prim.getAsBoolean());
            } else {
              defaults.put(entry.getKey(), prim.getAsString());
            }
          }
        }
      }

      builder.addNode(id, type, defaults);
    }

    var edgesArray = template.getAsJsonArray("edges");
    assertNotNull(edgesArray, "Template " + filename + " must have an 'edges' array");

    for (var element : edgesArray) {
      var edge = element.getAsJsonObject();
      var source = edge.get("source").getAsString();
      var sourceHandle = edge.get("sourceHandle").getAsString();
      var target = edge.get("target").getAsString();
      var targetHandle = edge.get("targetHandle").getAsString();
      var edgeType = edge.get("edgeType").getAsString();

      if ("EXECUTION".equals(edgeType)) {
        builder.addExecutionEdge(source, sourceHandle, target, targetHandle);
      } else {
        builder.addDataEdge(source, sourceHandle, target, targetHandle);
      }
    }

    var graph = assertDoesNotThrow(builder::build,
      "Template " + filename + " should build without validation errors");

    assertNotNull(graph, "Built graph should not be null");
    assertEquals(nodesArray.size(), graph.nodes().size(),
      "Built graph should have same node count as template");
    assertEquals(edgesArray.size(), graph.edges().size(),
      "Built graph should have same edge count as template");
  }

  private JsonObject loadTemplate(String filename) throws IOException {
    var resourcePath = "/templates/" + filename;
    try (var stream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(stream, "Template resource not found: " + resourcePath);
      try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        return GSON.fromJson(reader, JsonObject.class);
      }
    }
  }
}
