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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/// Flow control node that performs a multi-way branch based on a value.
/// Input: value (the value to switch on)
/// Input: cases (comma-separated list of case values)
/// Output: branch (string indicating which case matched, or "default")
///
/// The script executor should use the output to determine which branch to execute.
public final class SwitchNode extends AbstractScriptNode {
  private static final int MAX_CASES = 8;
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.switch")
    .displayName("Switch")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("value", "Value", PortType.STRING, "\"\"", "Value to switch on"),
      PortDefinition.inputWithDefault("cases", "Cases", PortType.STRING, "\"case1,case2,case3\"", "Comma-separated case values")
    )
    .addOutputs(
      PortDefinition.output(StandardPorts.EXEC_DEFAULT, "Default", PortType.EXEC, "Executes if no case matches"),
      PortDefinition.output(StandardPorts.exec("case0"), "Case 0", PortType.EXEC, "Executes if case 0 matches"),
      PortDefinition.output(StandardPorts.exec("case1"), "Case 1", PortType.EXEC, "Executes if case 1 matches"),
      PortDefinition.output(StandardPorts.exec("case2"), "Case 2", PortType.EXEC, "Executes if case 2 matches"),
      PortDefinition.output(StandardPorts.exec("case3"), "Case 3", PortType.EXEC, "Executes if case 3 matches"),
      PortDefinition.output(StandardPorts.exec("case4"), "Case 4", PortType.EXEC, "Executes if case 4 matches"),
      PortDefinition.output(StandardPorts.exec("case5"), "Case 5", PortType.EXEC, "Executes if case 5 matches"),
      PortDefinition.output(StandardPorts.exec("case6"), "Case 6", PortType.EXEC, "Executes if case 6 matches"),
      PortDefinition.output(StandardPorts.exec("case7"), "Case 7", PortType.EXEC, "Executes if case 7 matches"),
      PortDefinition.output("branch", "Branch", PortType.STRING, "Which case matched"),
      PortDefinition.output("caseIndex", "Case Index", PortType.NUMBER, "Index of matched case (-1 for default)"),
      PortDefinition.output("matched", "Matched", PortType.BOOLEAN, "Whether any case matched")
    )
    .description("Performs a multi-way branch based on a value")
    .icon("git-branch")
    .color("#607D8B")
    .addKeywords("switch", "case", "branch", "select")
    .supportsMuting(false)
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getStringInput(inputs, "value", "");
    var casesStr = getStringInput(inputs, "cases", "");

    var cases = casesStr.split(",");
    var caseCount = Math.min(cases.length, MAX_CASES);
    for (int i = 0; i < caseCount; i++) {
      if (cases[i].trim().equals(value)) {
        var outputs = new HashMap<String, NodeValue>();
        outputs.put("branch", NodeValue.of("case" + i));
        outputs.put("caseIndex", NodeValue.of(i));
        outputs.put("matched", NodeValue.of(true));
        outputs.put(StandardPorts.exec("case" + i), NodeValue.ofBoolean(true));
        return completedMono(outputs);
      }
    }

    // No case matched, return default
    var outputs = new HashMap<String, NodeValue>();
    outputs.put("branch", NodeValue.of("default"));
    outputs.put("caseIndex", NodeValue.of(-1));
    outputs.put("matched", NodeValue.of(false));
    outputs.put(StandardPorts.EXEC_DEFAULT, NodeValue.ofBoolean(true));
    return completedMono(outputs);
  }
}
