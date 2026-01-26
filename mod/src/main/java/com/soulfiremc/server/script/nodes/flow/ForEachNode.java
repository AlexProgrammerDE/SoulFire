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
import com.soulfiremc.server.script.ScriptContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that iterates over a collection.
/// Input: items (list of items to iterate over)
/// Input: currentIndex (current iteration, managed by script executor)
/// Output: item (current item), index (current index)
/// Output: isComplete (boolean, true when iteration is done)
///
/// The script executor is responsible for managing the iteration state.
public final class ForEachNode extends AbstractScriptNode {
  public static final String TYPE = "flow.foreach";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("items", NodeValue.ofList(List.of()), "currentIndex", NodeValue.ofNumber(0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var items = getListInput(inputs, "items");
    var currentIndex = getIntInput(inputs, "currentIndex", 0);

    var isComplete = currentIndex >= items.size();
    var currentItem = isComplete ? NodeValue.ofNull() : items.get(currentIndex);

    return completed(results(
      "item", currentItem,
      "index", currentIndex,
      "isComplete", isComplete,
      "nextIndex", currentIndex + 1,
      "size", items.size()
    ));
  }
}
