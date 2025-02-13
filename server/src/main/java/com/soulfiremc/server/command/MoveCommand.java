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
import com.soulfiremc.server.protocol.bot.ControllingTask;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class MoveCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("move")
        .then(
          literal("forward")
            .executes(
              help(
                "Toggle walking forward for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.forward(!controlState.forward());
                        controlState.backward(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("backward")
            .executes(
              help(
                "Toggle walking backward for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.backward(!controlState.backward());
                        controlState.forward(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("left")
            .executes(
              help(
                "Toggle walking left for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.left(!controlState.left());
                        controlState.right(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("right")
            .executes(
              help(
                "Toggle walking right for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.right(!controlState.right());
                        controlState.left(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    })))));
  }
}
