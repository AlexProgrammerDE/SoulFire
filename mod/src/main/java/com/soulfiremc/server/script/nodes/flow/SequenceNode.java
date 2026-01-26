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

/// Flow control node that executes multiple branches in sequence.
/// This node simply passes execution through and is used to organize sequential execution flow.
/// The script executor handles connecting multiple outputs from this node to execute them in order.
///
/// Output: out0, out1, out2, etc. (execution outputs for each branch)
public final class SequenceNode extends AbstractScriptNode {
  public static final String TYPE = "flow.sequence";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("branchCount", NodeValue.ofNumber(2));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var branchCount = getIntInput(inputs, "branchCount", 2);

    // Simply output the branch count so the executor knows how many branches to execute
    return completed(result("branchCount", branchCount));
  }
}
