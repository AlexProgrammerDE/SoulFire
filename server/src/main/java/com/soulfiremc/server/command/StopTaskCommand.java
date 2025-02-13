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

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class StopTaskCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("stop-task")
        .executes(
          help(
            "Makes selected bots stop their current task",
            c ->
              forEveryBot(
                c,
                bot -> {
                  if (bot.botControl().stopControllingTask()) {
                    c.getSource().source().sendInfo("Stopped current task for " + bot.accountName());
                  } else {
                    c.getSource().source().sendWarn("No task was running!");
                  }

                  return Command.SINGLE_SUCCESS;
                }))));
  }
}
