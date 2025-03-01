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

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.help;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.literal;

public final class GenerateTokenCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("generate-token")
        .executes(
          help(
            "Generate an auth JWT for the current user",
            c -> {
              var authSystem = c.getSource().soulFire().authSystem();
              c.getSource().source().sendInfo(
                "JWT (This gives full access to your user, make sure you only give this to trusted users): {}",
                authSystem.generateJWT(authSystem.getUserData(c.getSource().source().getUniqueId()).orElseThrow()));

              return Command.SINGLE_SUCCESS;
            })));
  }
}
