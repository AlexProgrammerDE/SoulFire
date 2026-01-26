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

/// Flow control node that repeats execution a specified number of times.
/// Input: count (number of iterations)
/// Input: currentIndex (current iteration, managed by script executor)
/// Output: index (current iteration index, 0-based)
/// Output: isComplete (boolean, true when all iterations are done)
///
/// The script executor is responsible for managing the loop state and re-executing
/// the connected nodes for each iteration.
public final class LoopNode extends AbstractScriptNode {
  public static final String TYPE = "flow.loop";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("count", NodeValue.ofNumber(10), "currentIndex", NodeValue.ofNumber(0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var count = getIntInput(inputs, "count", 10);
    var currentIndex = getIntInput(inputs, "currentIndex", 0);

    var isComplete = currentIndex >= count;

    return completed(results(
      "index", currentIndex,
      "isComplete", isComplete,
      "nextIndex", currentIndex + 1
    ));
  }
}
