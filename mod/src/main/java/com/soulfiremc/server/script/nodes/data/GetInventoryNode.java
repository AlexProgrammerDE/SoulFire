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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets information about an item in a specific inventory slot.
/// Input: slot (0-35 for main inventory, 36-39 for armor, 40 for offhand)
/// Outputs: itemId (string), count (int), isEmpty (boolean)
public final class GetInventoryNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_inventory")
    .displayName("Get Inventory")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get inventory from"),
      PortDefinition.inputWithDefault("slot", "Slot", PortType.NUMBER, "0", "Inventory slot (0-35 main, 36-39 armor, 40 offhand)")
    )
    .addOutputs(
      PortDefinition.output("itemId", "Item ID", PortType.STRING, "The item's registry ID"),
      PortDefinition.output("count", "Count", PortType.NUMBER, "Stack count"),
      PortDefinition.output("isEmpty", "Is Empty", PortType.BOOLEAN, "Whether the slot is empty")
    )
    .description("Gets information about an item in a specific inventory slot")
    .icon("backpack")
    .color("#9C27B0")
    .addKeywords("inventory", "item", "slot", "count", "stack")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;
    var slot = getIntInput(inputs, "slot", 0);

    if (player == null) {
      return completed(results("itemId", "minecraft:air", "count", 0, "isEmpty", true));
    }

    var inventory = player.getInventory();
    var item = inventory.getItem(slot);

    if (item.isEmpty()) {
      return completed(results("itemId", "minecraft:air", "count", 0, "isEmpty", true));
    }

    return completed(results(
      "itemId", item.getItemHolder().getRegisteredName(),
      "count", item.getCount(),
      "isEmpty", false
    ));
  }
}
