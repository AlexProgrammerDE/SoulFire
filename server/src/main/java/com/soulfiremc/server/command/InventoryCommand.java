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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryMenu;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class InventoryCommand {
  private static String format(SFItemStack item) {
    return item.isEmpty() ? "empty" : item.type().key() + " x" + item.getCount();
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("inventory")
        .then(
          literal("close")
            .executes(
              help(
                "Closes the current inventory for selected bots, opens the player inventory afterwards",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> bot.dataManager().localPlayer().closeContainer()));
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
                      var container = bot.dataManager().localPlayer().currentContainer;
                      switch (container) {
                        case WindowContainer windowContainer -> c.getSource().source().sendInfo("Current inventory type: " + windowContainer.containerType() + " with title " + windowContainer.title());
                        case PlayerInventoryMenu ignored -> c.getSource().source().sendInfo("Current inventory: Player inventory");
                        default -> c.getSource().source().sendInfo("Current inventory: Unknown");
                      }

                      c.getSource().source().sendInfo("Carried item: " + format(container.getCarried()));
                      for (var slot : container.slots()) {
                        c.getSource().source().sendInfo("Slot " + slot.slot() + ": " + format(slot.item()));
                      }

                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("left-click")
            .then(
              argument("slot", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Left-clicks the specified slot in the current inventory for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = IntegerArgumentType.getInteger(c, "slot");
                          var container = bot.dataManager().localPlayer().currentContainer;
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(
                            () -> container.leftClick(container.getSlot(slot))));
                          return Command.SINGLE_SUCCESS;
                        })))))
        .then(
          literal("right-click")
            .then(
              argument("slot", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Right-clicks the specified slot in the current inventory for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = IntegerArgumentType.getInteger(c, "slot");
                          var container = bot.dataManager().localPlayer().currentContainer;
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(
                            () -> container.rightClick(container.getSlot(slot))));
                          return Command.SINGLE_SUCCESS;
                        })))))
        .then(
          literal("left-click-outside")
            .executes(
              help(
                "Left-clicks outside current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var container = bot.dataManager().localPlayer().currentContainer;
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(
                        container::leftClickOutsideInventory));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("right-click-outside")
            .executes(
              help(
                "Right-clicks outside current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var container = bot.dataManager().localPlayer().currentContainer;
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(
                        container::rightClickOutsideInventory));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("middle-click")
            .then(
              argument("slot", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Middle-clicks the specified slot in the current inventory for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = IntegerArgumentType.getInteger(c, "slot");
                          var container = bot.dataManager().localPlayer().currentContainer;
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(
                            () -> container.middleClick(container.getSlot(slot))));
                          return Command.SINGLE_SUCCESS;
                        })))))
        .then(
          literal("drop-one")
            .then(
              argument("slot", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Drops one item from the specified slot in the current inventory for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = IntegerArgumentType.getInteger(c, "slot");
                          var container = bot.dataManager().localPlayer().currentContainer;
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(
                            () -> container.dropOne(container.getSlot(slot))));
                          return Command.SINGLE_SUCCESS;
                        })))))
        .then(
          literal("drop-all")
            .then(
              argument("slot", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .executes(
                  help(
                    "Drops all items from the specified slot in the current inventory for selected bots",
                    c ->
                      forEveryBot(
                        c,
                        bot -> {
                          var slot = IntegerArgumentType.getInteger(c, "slot");
                          var container = bot.dataManager().localPlayer().currentContainer;
                          bot.botControl().registerControllingTask(ControllingTask.singleTick(
                            () -> container.dropFullStack(container.getSlot(slot))));
                          return Command.SINGLE_SUCCESS;
                        }))))));
  }
}
