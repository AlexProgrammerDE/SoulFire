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
package com.soulfiremc.server.script.nodes.data;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's current rotation.
/// Outputs: yaw, pitch (float, in degrees)
public final class GetRotationNode extends AbstractScriptNode {
  public static final String TYPE = "data.get_rotation";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completed(results("yaw", 0.0f, "pitch", 0.0f));
    }

    return completed(results(
      "yaw", player.getYRot(),
      "pitch", player.getXRot()
    ));
  }
}
