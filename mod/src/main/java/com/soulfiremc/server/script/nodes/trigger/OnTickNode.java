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
package com.soulfiremc.server.script.nodes.trigger;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Trigger node that fires every game tick (20 times per second).
/// Output: tickCount - the current tick count since the script started
public final class OnTickNode extends AbstractScriptNode {
  public static final String TYPE = "trigger.on_tick";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isTrigger() {
    return true;
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    // The tick count is passed through the inputs from the trigger system
    var tickCount = getLongInput(inputs, "tickCount", 0L);
    return completed(result("tickCount", tickCount));
  }
}
