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
import com.soulfiremc.server.api.SessionLifecycle;
import com.soulfiremc.server.command.CommandSourceStack;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class SessionCommand {
  private SessionCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("session")
        .then(
          literal("start")
            .executes(
              help(
                "Makes selected instances start a session",
                c ->
                  forEveryInstance(
                    c,
                    instance -> {
                      instance.switchToState(c.getSource().source(), SessionLifecycle.RUNNING);

                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("stop")
            .executes(
              help(
                "Makes selected instances stop their session",
                c ->
                  forEveryInstance(
                    c,
                    instance -> {
                      instance.switchToState(c.getSource().source(), SessionLifecycle.STOPPED);

                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("pause")
            .executes(
              help(
                "Makes selected instances pause their session",
                c ->
                  forEveryInstance(
                    c,
                    instance -> {
                      instance.switchToState(c.getSource().source(), SessionLifecycle.PAUSED);

                      return Command.SINGLE_SUCCESS;
                    })))
        )
    );
  }
}
