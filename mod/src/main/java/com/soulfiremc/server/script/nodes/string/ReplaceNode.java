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
import com.soulfiremc.server.script.NodeRuntime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// String node that replaces occurrences of a substring.
/// Inputs: text, search, replacement, replaceAll (boolean)
/// Output: result
public final class ReplaceNode extends AbstractScriptNode {
  public static final String TYPE = "string.replace";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of(
      "text", NodeValue.ofString(""),
      "search", NodeValue.ofString(""),
      "replacement", NodeValue.ofString(""),
      "replaceAll", NodeValue.ofBoolean(true)
    );
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var search = getStringInput(inputs, "search", "");
    var replacement = getStringInput(inputs, "replacement", "");
    var replaceAll = getBooleanInput(inputs, "replaceAll", true);

    String resultText;
    if (search.isEmpty()) {
      resultText = text;
    } else if (replaceAll) {
      resultText = text.replace(search, replacement);
    } else {
      resultText = text.replaceFirst(java.util.regex.Pattern.quote(search), java.util.regex.Matcher.quoteReplacement(replacement));
    }

    return completed(result("result", resultText));
  }
}
