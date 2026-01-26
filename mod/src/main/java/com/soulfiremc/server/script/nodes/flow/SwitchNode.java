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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.NodeRuntime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that performs a multi-way branch based on a value.
/// Input: value (the value to switch on)
/// Input: cases (comma-separated list of case values)
/// Output: branch (string indicating which case matched, or "default")
///
/// The script executor should use the output to determine which branch to execute.
public final class SwitchNode extends AbstractScriptNode {
  public static final String TYPE = "flow.switch";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("value", NodeValue.ofString(""), "cases", NodeValue.ofString("case1,case2,case3"));
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
