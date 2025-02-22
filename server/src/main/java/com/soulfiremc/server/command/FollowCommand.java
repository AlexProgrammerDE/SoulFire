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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.soulfiremc.server.command.brigadier.ArgumentTypeHelper;
import com.soulfiremc.server.pathfinding.controller.FollowEntityController;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class FollowCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("follow")
        .then(argument("entity", StringArgumentType.string())
          .then(argument("maxRadius", IntegerArgumentType.integer(1))
            .executes(
              help(
                "Makes selected bots follow an entity by id",
                c -> {
                  var entityName = StringArgumentType.getString(c, "entity");
                  var maxRadius = IntegerArgumentType.getInteger(c, "maxRadius");

                  return forEveryBot(
                    c,
                    bot -> {
                      var entityMatcher = ArgumentTypeHelper.parseEntityMatch(bot, entityName);
                      bot.scheduler().schedule(() -> new FollowEntityController(
                        entityMatcher,
                        maxRadius
                      ).start(bot));

                      return Command.SINGLE_SUCCESS;
                    });
                })))));
  }
}
