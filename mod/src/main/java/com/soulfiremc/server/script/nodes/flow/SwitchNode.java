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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that performs a multi-way branch based on a value.
/// Input: value (the value to switch on)
/// Input: cases (comma-separated list of case values)
/// Output: branch (string indicating which case matched, or "default")
///
/// The script executor should use the output to determine which branch to execute.
public final class SwitchNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.switch")
    .displayName("Switch")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("value", "Value", PortType.STRING, "\"\"", "Value to switch on"),
      PortDefinition.inputWithDefault("cases", "Cases", PortType.STRING, "\"case1,case2,case3\"", "Comma-separated case values")
    )
    .addOutputs(
      PortDefinition.output("exec_default", "Default", PortType.EXEC, "Executes if no case matches"),
      PortDefinition.output("exec_case0", "Case 0", PortType.EXEC, "Executes if first case matches"),
      PortDefinition.output("exec_case1", "Case 1", PortType.EXEC, "Executes if second case matches"),
      PortDefinition.output("exec_case2", "Case 2", PortType.EXEC, "Executes if third case matches"),
      PortDefinition.output("branch", "Branch", PortType.STRING, "Which case matched"),
      PortDefinition.output("caseIndex", "Case Index", PortType.NUMBER, "Index of matched case (-1 for default)"),
      PortDefinition.output("matched", "Matched", PortType.BOOLEAN, "Whether any case matched")
    )
    .description("Performs a multi-way branch based on a value")
    .icon("git-branch")
    .color("#607D8B")
    .addKeywords("switch", "case", "branch", "select")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getStringInput(inputs, "value", "");
    var casesStr = getStringInput(inputs, "cases", "");

    var cases = casesStr.split(",");
    for (int i = 0; i < cases.length; i++) {
      if (cases[i].trim().equals(value)) {
        return completed(results(
          "branch", "case" + i,
          "caseIndex", i,
          "matched", true
        ));
      }
    }

    // No case matched, return default
    return completed(results(
      "branch", "default",
      "caseIndex", -1,
      "matched", false
    ));
  }
}
