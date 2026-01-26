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

import com.soulfiremc.server.script.*;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's currently selected hotbar slot.
/// Outputs: slot (0-8), itemId, itemCount
public final class GetSelectedSlotNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_selected_slot")
    .displayName("Get Selected Slot")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get selected slot from")
    )
    .addOutputs(
      PortDefinition.output("slot", "Slot", PortType.NUMBER, "Selected hotbar slot (0-8)"),
      PortDefinition.output("itemId", "Item ID", PortType.STRING, "Item in selected slot"),
      PortDefinition.output("itemCount", "Count", PortType.NUMBER, "Stack count in selected slot")
    )
    .description("Gets the bot's currently selected hotbar slot and item")
    .icon("hand")
    .color("#9C27B0")
    .addKeywords("slot", "hotbar", "selected", "hand", "held")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completed(results("slot", 0, "itemId", "minecraft:air", "itemCount", 0));
    }

    var inventory = player.getInventory();
    var selectedSlot = inventory.selected;
    var heldItem = inventory.getSelected();
    var itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();

    return completed(results(
      "slot", selectedSlot,
      "itemId", itemId,
      "itemCount", heldItem.getCount()
    ));
  }
}
