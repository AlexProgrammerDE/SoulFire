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
package com.soulfiremc.server.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.database.UserEntity;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class SetEmailCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("set-email")
        .then(argument("email", StringArgumentType.greedyString())
          .executes(
            help(
              "Set the email of the current user",
              c -> {
                var email = StringArgumentType.getString(c, "email");
                c.getSource().soulFire().sessionFactory().inTransaction(s -> {
                  var userData = s.find(UserEntity.class, c.getSource().source().getUniqueId());
                  userData.email(email);
                  s.merge(userData);
                });
                c.getSource().source().sendInfo("Email of user {} set to {}", c.getSource().source().getUsername(), email);

                return Command.SINGLE_SUCCESS;
              }))));
  }
}
