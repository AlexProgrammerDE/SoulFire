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
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Pattern;

/// String node that replaces matches of a regex pattern with a replacement string.
public final class RegexReplaceNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.regex_replace")
    .displayName("Regex Replace")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "String to modify"),
      PortDefinition.input("pattern", "Pattern", PortType.STRING, "Regex pattern to match"),
      PortDefinition.input("replacement", "Replacement", PortType.STRING, "Replacement string (supports $1, $2 for groups)"),
      PortDefinition.inputWithDefault("replaceAll", "Replace All", PortType.BOOLEAN, "true", "Replace all matches or just first"),
      PortDefinition.inputWithDefault("flags", "Flags", PortType.STRING, "\"\"", "Regex flags")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "String with replacements made"),
      PortDefinition.output("count", "Count", PortType.NUMBER, "Number of replacements made")
    )
    .description("Replaces matches of a regex pattern with a replacement string")
    .icon("replace")
    .color("#EAB308")
    .addKeywords("regex", "replace", "substitute", "pattern", "sed")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var patternStr = getStringInput(inputs, "pattern", "");
    var replacement = getStringInput(inputs, "replacement", "");
    var replaceAll = getBooleanInput(inputs, "replaceAll", true);
    var flags = getStringInput(inputs, "flags", "");

    if (patternStr.isEmpty()) {
      return completedMono(results(
        "result", input,
        "count", 0
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

      // Count matches first
      int count = 0;
      while (matcher.find()) {
        count++;
        if (!replaceAll) {
          break;
        }
      }
      matcher.reset();

      String result;
      if (replaceAll) {
        result = matcher.replaceAll(replacement);
      } else {
        result = matcher.replaceFirst(replacement);
      }

      return completedMono(results(
        "result", result,
        "count", count
      ));
    } catch (Exception _) {
      return completedMono(results(
        "result", input,
        "count", 0
      ));
    }
  }
}
