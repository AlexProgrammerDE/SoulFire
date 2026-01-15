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
package com.soulfiremc.server.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.soulfiremc.server.command.CommandSourceStack;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class HealthCommand {
  private HealthCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("health")
        .executes(
          help(
            "Print the health of selected bots.",
            c -> forEveryBot(
              c,
              bot -> {
                if (bot.minecraft().player == null) {
                  return Command.SINGLE_SUCCESS;
                }

                c.getSource().source().sendInfo("Info for " + bot.accountName() + ":");
                c.getSource().source().sendInfo("Health: " + bot.minecraft().player.getHealth() + " / " + bot.minecraft().player.getMaxHealth());
                c.getSource().source().sendInfo("Food: " + bot.minecraft().player.getFoodData().getFoodLevel());
                c.getSource().source().sendInfo("Saturation: " + bot.minecraft().player.getFoodData().getSaturationLevel());

                return Command.SINGLE_SUCCESS;
              }))));
  }
}
