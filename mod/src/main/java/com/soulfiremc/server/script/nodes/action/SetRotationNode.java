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
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.NodeRuntime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that sets the bot's rotation (yaw and pitch).
/// Inputs: yaw (degrees, -180 to 180), pitch (degrees, -90 to 90)
public final class SetRotationNode extends AbstractScriptNode {
  public static final String TYPE = "action.set_rotation";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("yaw", NodeValue.ofNumber(0.0), "pitch", NodeValue.ofNumber(0.0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var yaw = getFloatInput(inputs, "yaw", 0.0f);
    var pitch = getFloatInput(inputs, "pitch", 0.0f);

    // Clamp pitch to valid range
    pitch = Math.max(-90f, Math.min(90f, pitch));

    // Normalize yaw to -180 to 180 range
    while (yaw > 180f) yaw -= 360f;
    while (yaw < -180f) yaw += 360f;

    var finalYaw = yaw;
    var finalPitch = pitch;

    bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
      var player = bot.minecraft().player;
      if (player != null) {
        player.setYRot(finalYaw);
        player.setXRot(finalPitch);
      }
    }));

    return completedEmpty();
  }
}
