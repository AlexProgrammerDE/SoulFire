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
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.ControllingTask;
import net.kyori.adventure.text.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.event.Level;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class InventoryCommand {
  private static String format(ItemStack item) {
    return item.isEmpty() ? "empty" : item.getItemHolder().getRegisteredName() + " x" + item.getCount();
  }

  private static String format(Slot slot) {
    return "Slot " + slot.index + ": " + format(slot.getItem());
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
                      var player = bot.minecraft().player;
                      if (player == null) {
                        return Command.SINGLE_SUCCESS;
                      }

                      bot.botControl().registerControllingTask(ControllingTask.singleTick(player::closeContainer));
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
                      var source = c.getSource().source();
                      var player = bot.minecraft().player;
                      if (player == null) {
                        return Command.SINGLE_SUCCESS;
                      }

                      var container = player.containerMenu;
                      if (container instanceof InventoryMenu) {
                        source.sendInfo("Current inventory: Player inventory");
                      } else {
                        source.sendMessage(
                          Level.INFO,
                          Component.text("Current inventory type: ")
                            .append(Component.text(container.getClass().getSimpleName()))
                        );
                      }

                      source.sendInfo("Carried item: " + format(container.getCarried()));
                      if (container instanceof InventoryMenu playerInventoryMenu) {
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.RESULT_SLOT)) + " (Crafting result)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.CRAFT_SLOT_START)) + " (Top left crafting grid)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.CRAFT_SLOT_START + 1)) + " (Top right crafting grid)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.CRAFT_SLOT_START + 2)) + " (Bottom left crafting grid)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.CRAFT_SLOT_START + 3)) + " (Bottom right crafting grid:)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START)) + " (Helmet)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START + 1)) + " (Chestplate)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START + 2)) + " (Leggings)");
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START + 3)) + " (Boots)");
                        for (var i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                          var slot = playerInventoryMenu.getSlot(i);
                          source.sendInfo(format(slot) + " (Main inventory row " + (slot.index - 9) / 9 + ", column " + (slot.index - 9) % 9 + ")");
                        }
                        for (var i = InventoryMenu.USE_ROW_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
                          var slot = playerInventoryMenu.getSlot(i);
                          source.sendInfo(format(slot) + " (Hotbar " + (slot.index - 36) + ")");
                        }
                        source.sendInfo(format(playerInventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT)) + " (Offhand)");
                      } else {
                        for (var slot : container.slots) {
                          source.sendInfo(format(slot));
                        }
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
                          return performClick(bot, slot, 0, ClickType.PICKUP);
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
                          return performClick(bot, slot, 1, ClickType.PICKUP);
                        })))))
        .then(
          literal("left-click-outside")
            .executes(
              help(
                "Left-clicks outside current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> performClick(bot, -999, 0, ClickType.PICKUP)))))
        .then(
          literal("right-click-outside")
            .executes(
              help(
                "Right-clicks outside current inventory for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> performClick(bot, -999, 1, ClickType.PICKUP)))))
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
                          return performClick(bot, slot, 2, ClickType.CLONE);
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
                          return performClick(bot, slot, 0, ClickType.THROW);
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
                          return performClick(bot, slot, 1, ClickType.THROW);
                        }))))));
  }

  private static int performClick(BotConnection bot, int slotId, int mouseButton, ClickType clickType) {
    var player = bot.minecraft().player;
    var gameMode = bot.minecraft().gameMode;
    if (player == null || gameMode == null) {
      return Command.SINGLE_SUCCESS;
    }

    var container = player.containerMenu;
    bot.botControl().registerControllingTask(ControllingTask.singleTick(
      () -> gameMode.handleInventoryMouseClick(container.containerId, slotId, mouseButton, clickType, player)));
    return Command.SINGLE_SUCCESS;
  }
}
