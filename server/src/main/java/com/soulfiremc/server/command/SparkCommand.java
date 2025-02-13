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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.soulfiremc.server.spark.SFSparkCommandSender;
import com.soulfiremc.server.spark.SFSparkPlugin;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class SparkCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("spark")
        .then(argument("command", StringArgumentType.greedyString())
          .executes(
            help(
              "Runs a spark subcommand",
              c -> {
                var command = StringArgumentType.getString(c, "command");
                SFSparkPlugin.INSTANCE.platform()
                  .executeCommand(new SFSparkCommandSender(c.getSource().source()), command.split(ARGUMENT_SEPARATOR));
                return Command.SINGLE_SUCCESS;
              })))
        .executes(
          help(
            "Get spark help",
            c -> {
              SFSparkPlugin.INSTANCE.platform().executeCommand(new SFSparkCommandSender(c.getSource().source()), new String[]{});
              return Command.SINGLE_SUCCESS;
            })));
  }
}
