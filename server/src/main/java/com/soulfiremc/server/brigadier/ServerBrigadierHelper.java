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
package com.soulfiremc.server.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.soulfiremc.brigadier.CommandHelpWrapper;
import com.soulfiremc.brigadier.RedirectHelpWrapper;
import com.soulfiremc.server.user.ServerCommandSource;

public class ServerBrigadierHelper {
  private ServerBrigadierHelper() {}

  public static LiteralArgumentBuilder<ServerCommandSource> literal(String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  public static <T> RequiredArgumentBuilder<ServerCommandSource, T> argument(
    String name, ArgumentType<T> type) {
    return RequiredArgumentBuilder.argument(name, type);
  }

  public static Command<ServerCommandSource> help(String help, Command<ServerCommandSource> command) {
    return new CommandHelpWrapper<>(command, help, false);
  }

  public static RedirectModifier<ServerCommandSource> helpRedirect(
    String help, RedirectModifier<ServerCommandSource> redirect) {
    return new RedirectHelpWrapper<>(redirect, help, false);
  }

  public static Command<ServerCommandSource> privateCommand(Command<ServerCommandSource> command) {
    return new CommandHelpWrapper<>(command, null, true);
  }
}
