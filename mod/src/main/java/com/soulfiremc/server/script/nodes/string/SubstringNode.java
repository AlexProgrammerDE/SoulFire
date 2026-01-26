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
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// String node that extracts a substring.
/// Inputs: text, start (index), end (index, -1 for end of string)
/// Output: result
public final class SubstringNode extends AbstractScriptNode {
  public static final String TYPE = "string.substring";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("text", "", "start", 0.0, "end", -1.0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var text = getStringInput(inputs, "text", "");
    var start = getIntInput(inputs, "start", 0);
    var end = getIntInput(inputs, "end", -1);

    if (text.isEmpty()) {
      return completed(result("result", ""));
    }

    // Clamp start to valid range
    start = Math.max(0, Math.min(start, text.length()));

    // Handle end index
    if (end < 0) {
      end = text.length();
    } else {
      end = Math.min(end, text.length());
    }

    // Ensure start <= end
    if (start > end) {
      return completed(result("result", ""));
    }

    return completed(result("result", text.substring(start, end)));
  }
}
