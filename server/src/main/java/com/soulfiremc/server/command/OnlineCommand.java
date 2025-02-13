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

import java.util.ArrayList;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class OnlineCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("online")
        .executes(
          help(
            "Shows connected bots in attacks",
            c ->
              forEveryInstanceEnsureHasBots(
                c,
                instanceManager -> {
                  var online = new ArrayList<String>();
                  for (var bot : getVisibleBots(instanceManager, c)) {
                    if (bot.isOnline()) {
                      online.add(bot.accountName());
                    }
                  }

                  c.getSource().source()
                    .sendInfo(
                      online.size() + " bots online: " + String.join(", ", online));
                  return Command.SINGLE_SUCCESS;
                }))));
  }
}
