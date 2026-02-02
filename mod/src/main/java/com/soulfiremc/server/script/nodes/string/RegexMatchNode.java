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
package com.soulfiremc.server.script.nodes.string;

import com.soulfiremc.server.script.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/// String node that tests if a string matches a regex pattern and extracts capture groups.
public final class RegexMatchNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.regex_match")
    .displayName("Regex Match")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "String to test"),
      PortDefinition.input("pattern", "Pattern", PortType.STRING, "Regex pattern"),
      PortDefinition.inputWithDefault("flags", "Flags", PortType.STRING, "\"\"", "Regex flags (i=case-insensitive, m=multiline, s=dotall)")
    )
    .addOutputs(
      PortDefinition.output("matches", "Matches", PortType.BOOLEAN, "Whether the pattern matched"),
      PortDefinition.output("fullMatch", "Full Match", PortType.STRING, "The entire matched string"),
      PortDefinition.listOutput("groups", "Groups", PortType.STRING, "List of captured groups"),
      PortDefinition.listOutput("allMatches", "All Matches", PortType.STRING, "All matches found")
    )
    .description("Tests if a string matches a regex pattern and extracts capture groups")
    .icon("regex")
    .color("#EAB308")
    .addKeywords("regex", "match", "pattern", "test", "find", "capture", "group")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var patternStr = getStringInput(inputs, "pattern", "");
    var flags = getStringInput(inputs, "flags", "");

    if (patternStr.isEmpty()) {
      return completed(results(
        "matches", false,
        "fullMatch", "",
        "groups", List.of(),
        "allMatches", List.of()
      ));
    }

    try {
      int regexFlags = 0;
      if (flags.contains("i")) {
        regexFlags |= Pattern.CASE_INSENSITIVE;
      }
      if (flags.contains("m")) {
        regexFlags |= Pattern.MULTILINE;
      }
      if (flags.contains("s")) {
        regexFlags |= Pattern.DOTALL;
      }

      var compiledPattern = Pattern.compile(patternStr, regexFlags);
      var matcher = compiledPattern.matcher(input);

      if (matcher.find()) {
        var fullMatch = matcher.group(0);

        // Capture groups
        var groups = new ArrayList<NodeValue>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
          var group = matcher.group(i);
          groups.add(NodeValue.ofString(group != null ? group : ""));
        }

        // All matches
        var allMatches = new ArrayList<NodeValue>();
        allMatches.add(NodeValue.ofString(fullMatch));
        while (matcher.find()) {
          allMatches.add(NodeValue.ofString(matcher.group(0)));
        }

        return completed(results(
          "matches", true,
          "fullMatch", fullMatch,
          "groups", groups,
          "allMatches", allMatches
        ));
      }

      return completed(results(
        "matches", false,
        "fullMatch", "",
        "groups", List.of(),
        "allMatches", List.of()
      ));
    } catch (Exception _) {
      return completed(results(
        "matches", false,
        "fullMatch", "",
        "groups", List.of(),
        "allMatches", List.of()
      ));
    }
  }
}
