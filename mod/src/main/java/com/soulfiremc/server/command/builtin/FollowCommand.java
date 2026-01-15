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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.command.brigadier.EntityArgumentType;
import com.soulfiremc.server.pathfinding.controller.FollowEntityController;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class FollowCommand {
  private FollowCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("follow")
        .then(argument("entity", EntityArgumentType.INSTANCE)
          .then(argument("maxRadius", IntegerArgumentType.integer(1))
            .executes(
              help(
                "Makes selected bots follow an entity",
                c -> {
                  var entityMatcher = EntityArgumentType.getEntityMatcher(c, "entity");
                  var maxRadius = IntegerArgumentType.getInteger(c, "maxRadius");

                  return forEveryBot(
                    c,
                    bot -> {
                      bot.scheduler().schedule(() -> new FollowEntityController(
                        entityMatcher,
                        maxRadius
                      ).start(bot));

                      return Command.SINGLE_SUCCESS;
                    });
                })))));
  }
}
