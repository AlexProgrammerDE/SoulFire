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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.database.generated.Tables;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class SetEmailCommand {
  private SetEmailCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("set-email")
        .then(argument("email", StringArgumentType.greedyString())
          .executes(
            help(
              "Set the email of the current user",
              c -> {
                var email = StringArgumentType.getString(c, "email");
                c.getSource().soulFire().dsl()
                  .update(Tables.USERS)
                  .set(Tables.USERS.EMAIL, email)
                  .set(Tables.USERS.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                  .where(Tables.USERS.ID.eq(c.getSource().source().getUniqueId().toString()))
                  .execute();
                c.getSource().source().sendInfo("Email of user {} set to {}", c.getSource().source().getUsername(), email);

                return Command.SINGLE_SUCCESS;
              }))));
  }
}
