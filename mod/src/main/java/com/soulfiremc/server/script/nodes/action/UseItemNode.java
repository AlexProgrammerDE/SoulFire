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
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.util.MouseClickHelper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that uses the item in the bot's hand.
/// Right-clicks to use items, interact with entities, or interact with blocks.
public final class UseItemNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.use_item")
    .displayName("Use Item")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Uses the item in the bot's hand (right click)")
    .icon("mouse-pointer-click")
    .color("#FF9800")
    .addKeywords("use", "item", "right click", "interact", "eat", "drink")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);

    bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
      var minecraft = bot.minecraft();
      var player = minecraft.player;
      var level = minecraft.level;
      var gameMode = minecraft.gameMode;

      if (player != null && level != null && gameMode != null) {
        MouseClickHelper.performRightClick(player, level, gameMode);
      }
    }));

    return completedEmpty();
  }
}
