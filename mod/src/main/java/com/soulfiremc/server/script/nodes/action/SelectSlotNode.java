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

import com.soulfiremc.server.bot.ControllingTask;
import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that selects a hotbar slot (0-8).
/// Input: slot (0-8)
public final class SelectSlotNode extends AbstractScriptNode {
  public static final String TYPE = "action.select_slot";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("slot", 0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var bot = requireBot(inputs, context);
    var slot = getIntInput(inputs, "slot", 0);

    // Clamp slot to valid range
    slot = Math.max(0, Math.min(8, slot));
    var finalSlot = slot;

    bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
      var player = bot.minecraft().player;
      if (player != null) {
        player.getInventory().setSelectedSlot(finalSlot);
      }
    }));

    return completedEmpty();
  }
}
