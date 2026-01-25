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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/// Action node that delays execution for a specified duration.
/// Input: durationMs (milliseconds to wait)
public final class WaitNode extends AbstractScriptNode {
  public static final String TYPE = "action.wait";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("durationMs", 1000L);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var durationMs = getLongInput(inputs, "durationMs", 1000L);

    if (durationMs <= 0) {
      return completedEmpty();
    }

    var future = new CompletableFuture<Map<String, Object>>();

    context.scheduler().schedule(
      () -> future.complete(emptyResult()),
      durationMs,
      TimeUnit.MILLISECONDS
    );

    return future;
  }
}
