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
import com.soulfiremc.server.util.RPCConstants;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.help;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.literal;

public final class GenerateTokenCommand {
  private GenerateTokenCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("generate-token")
        .then(literal("api")
          .executes(
            help(
              "Generate an API JWT for the current user",
              c -> {
                var authSystem = c.getSource().soulFire().authSystem();
                c.getSource().source().sendInfo(
                  "JWT (This gives full access to your account, keep this secret): {}",
                  authSystem.generateJWT(
                    authSystem.getUserData(c.getSource().source().getUniqueId()).orElseThrow(),
                    RPCConstants.API_AUDIENCE
                  ));

                return Command.SINGLE_SUCCESS;
              })))
        .then(literal("webdav")
          .executes(
            help(
              "Generate a WebDAV JWT for the current user",
              c -> {
                var authSystem = c.getSource().soulFire().authSystem();
                c.getSource().source().sendInfo(
                  "JWT (This gives full access to your WebDAV files, keep this secret): {}",
                  authSystem.generateJWT(
                    authSystem.getUserData(c.getSource().source().getUniqueId()).orElseThrow(),
                    RPCConstants.API_AUDIENCE
                  ));

                return Command.SINGLE_SUCCESS;
              }))));
  }
}
