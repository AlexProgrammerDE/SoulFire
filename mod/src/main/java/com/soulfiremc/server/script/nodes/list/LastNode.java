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
import com.soulfiremc.server.script.NodeRuntime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// List node that gets the last item in a list.
/// Input: list
/// Outputs: item, found (boolean)
public final class LastNode extends AbstractScriptNode {
  public static final String TYPE = "list.last";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("list", NodeValue.ofList(List.of()));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var list = getListInput(inputs, "list");

    if (!list.isEmpty()) {
      return completed(results("item", list.getLast(), "found", true));
    } else {
      return completed(results("item", null, "found", false));
    }
  }
}
