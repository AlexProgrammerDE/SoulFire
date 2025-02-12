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
package com.soulfiremc.server.command.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.server.command.CommandSourceStack;

import java.util.Collection;

public class ServerBrigadierHelper {
  private ServerBrigadierHelper() {}

  public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(
    String name, ArgumentType<T> type) {
    return RequiredArgumentBuilder.argument(name, type);
  }

  public static Command<CommandSourceStack> help(String help, Command<CommandSourceStack> command) {
    return new CommandHelpWrapper(command, help, false);
  }

  public static RedirectModifier<CommandSourceStack> helpRedirect(
    String help, RedirectModifier<CommandSourceStack> redirect) {
    return new RedirectHelpWrapper(redirect, help, false);
  }

  public static SingleRedirectModifier<CommandSourceStack> helpSingleRedirect(
    String help, SingleRedirectModifier<CommandSourceStack> redirect) {
    return new SingleRedirectHelpWrapper(redirect, help, false);
  }

  public static Command<CommandSourceStack> privateCommand(Command<CommandSourceStack> command) {
    return new CommandHelpWrapper(command, null, true);
  }

  public sealed interface HelpCarrier permits SingleRedirectHelpWrapper, CommandHelpWrapper, RedirectHelpWrapper {
    String help();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean privateCommand();
  }

  private record SingleRedirectHelpWrapper(
    SingleRedirectModifier<CommandSourceStack> command,
    String help,
    boolean privateCommand
  ) implements SingleRedirectModifier<CommandSourceStack>, HelpCarrier {
    @Override
    public CommandSourceStack apply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return command.apply(context);
    }
  }

  private record CommandHelpWrapper(Command<CommandSourceStack> command, String help, boolean privateCommand)
    implements Command<CommandSourceStack>, HelpCarrier {
    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return command.run(context);
    }
  }

  private record RedirectHelpWrapper(
    RedirectModifier<CommandSourceStack> command,
    String help,
    boolean privateCommand
  ) implements RedirectModifier<CommandSourceStack>, HelpCarrier {
    @Override
    public Collection<CommandSourceStack> apply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return command.apply(context);
    }
  }
}
