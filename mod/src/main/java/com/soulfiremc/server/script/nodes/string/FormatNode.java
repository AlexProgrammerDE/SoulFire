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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// String node that formats a template string with placeholders.
/// Placeholders use {0}, {1}, {2}, etc. syntax.
/// Inputs: template, args (list of values)
/// Output: result
public final class FormatNode extends AbstractScriptNode {
  public static final String TYPE = "string.format";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("template", NodeValue.ofString(""), "args", NodeValue.ofList(List.of()));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var template = getStringInput(inputs, "template", "");
    var args = getListInput(inputs, "args");

    var result = template;
    for (int i = 0; i < args.size(); i++) {
      var placeholder = "{" + i + "}";
      var value = args.get(i);
      result = result.replace(placeholder, value != null ? value.toString() : "null");
    }

    return completed(result("result", result));
  }
}
