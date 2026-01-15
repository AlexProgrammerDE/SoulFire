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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.soulfiremc.server.command.CommandSourceStack;

import java.util.ArrayList;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class RepeatCommand {
  private RepeatCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("repeat")
        .then(
          argument("amount", IntegerArgumentType.integer(1))
            .forward(
              dispatcher.getRoot(),
              helpRedirect(
                "Repeat the command for the specified amount of times",
                c -> {
                  var amount = IntegerArgumentType.getInteger(c, "amount");
                  var list = new ArrayList<CommandSourceStack>();
                  for (var i = 0; i < amount; i++) {
                    list.add(c.getSource());
                  }

                  return list;
                }),
              true
            )));
  }
}
