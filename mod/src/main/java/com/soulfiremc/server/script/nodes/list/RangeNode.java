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
package com.soulfiremc.server.script.nodes.list;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// List node that generates a range of numbers.
/// Inputs: start, end, step
/// Output: list
public final class RangeNode extends AbstractScriptNode {
  public static final String TYPE = "list.range";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("start", NodeValue.ofNumber(0.0), "end", NodeValue.ofNumber(10.0), "step", NodeValue.ofNumber(1.0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var start = getDoubleInput(inputs, "start", 0.0);
    var end = getDoubleInput(inputs, "end", 10.0);
    var step = getDoubleInput(inputs, "step", 1.0);

    // Prevent infinite loops
    if (step == 0 || (step > 0 && start > end) || (step < 0 && start < end)) {
      return completed(result("list", List.of()));
    }

    // Limit to 10000 elements to prevent memory issues
    var maxElements = 10000;
    var list = new ArrayList<Double>();

    if (step > 0) {
      for (double i = start; i < end && list.size() < maxElements; i += step) {
        list.add(i);
      }
    } else {
      for (double i = start; i > end && list.size() < maxElements; i += step) {
        list.add(i);
      }
    }

    return completed(result("list", list));
  }
}
