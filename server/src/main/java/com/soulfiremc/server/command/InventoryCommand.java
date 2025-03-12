/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class InventoryCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("inventory")
        .then(
          literal("close")
            .executes(
              help(
                "Closes the current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> bot.inventoryManager().closeInventory()));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("current")
            .executes(
              help(
                "Get info about the current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var inventory = bot.inventoryManager().currentContainer();
                      switch (inventory) {
                        case null -> {
                          c.getSource().source().sendInfo("No inventory is currently open");
                          return Command.SINGLE_SUCCESS;
                        }
                        case WindowContainer windowContainer -> c.getSource().source().sendInfo("Current inventory type: " + windowContainer.containerType() + " with title " + windowContainer.title());
                        case PlayerInventoryContainer ignored -> c.getSource().source().sendInfo("Current inventory: Player inventory");
                        default -> c.getSource().source().sendInfo("Current inventory: Unknown");
                      }

                      for (var slot : inventory.getSlots(0, inventory.slots().length - 1)) {
                        c.getSource().source().sendInfo("Slot " + slot.slot() + ": " + slot.item());
                      }

                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("left-click")
            .then(
              argument("slot", integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Drops the item in the specified slot for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = c.getArgument("slot", Integer.class);
                          var inventoryManager = bot.inventoryManager();
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                            var inventory = inventoryManager.currentContainer();
                            if (inventory == null) {
                              c.getSource().source().sendInfo("No inventory is currently open");
                              return;
                            }

                            inventoryManager.leftClickSlot(inventory.getSlot(slot));
                          }));
                          return Command.SINGLE_SUCCESS;
                        }))))));
  }
}
