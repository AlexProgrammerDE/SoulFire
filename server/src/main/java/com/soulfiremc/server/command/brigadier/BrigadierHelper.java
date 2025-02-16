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

import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.protocol.BotConnection;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;

public class BrigadierHelper {
  private BrigadierHelper() {}

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

  public static HelpData[] getAllUsage(
    final CommandDispatcher<CommandSourceStack> dispatcher,
    final CommandNode<CommandSourceStack> node,
    final CommandSourceStack source) {
    final var result = new ArrayList<HelpData>();
    getAllUsage(dispatcher, node, source, result, "");
    return result.toArray(new HelpData[0]);
  }

  private static void getAllUsage(
    final CommandDispatcher<CommandSourceStack> dispatcher,
    final CommandNode<CommandSourceStack> node,
    final CommandSourceStack source,
    final ArrayList<HelpData> result,
    final String prefix) {
    if (!node.canUse(source)) {
      return;
    }

    if (node.getCommand() != null) {
      var helpWrapper = (BrigadierHelper.HelpCarrier) node.getCommand();
      if (!helpWrapper.privateCommand()) {
        result.add(new HelpData(prefix, helpWrapper.help()));
      }
    }

    if (node.getRedirect() != null) {
      var redirectHelpWrapper = (BrigadierHelper.HelpCarrier) node.getRedirectModifier();
      if (!redirectHelpWrapper.privateCommand()) {
        final var redirect =
          node.getRedirect() == dispatcher.getRoot()
            ? "..."
            : "-> " + node.getRedirect().getUsageText();
        result.add(
          new HelpData(
            prefix.isEmpty()
              ? node.getUsageText() + ARGUMENT_SEPARATOR + redirect
              : prefix + ARGUMENT_SEPARATOR + redirect,
            redirectHelpWrapper.help()));
      }
    } else if (!node.getChildren().isEmpty()) {
      for (final var child : node.getChildren()) {
        getAllUsage(
          dispatcher,
          child,
          source,
          result,
          prefix.isEmpty()
            ? child.getUsageText()
            : prefix + ARGUMENT_SEPARATOR + child.getUsageText());
      }
    }
  }

  public static List<InstanceManager> getVisibleInstances(CommandContext<CommandSourceStack> context) {
    return context.getSource().soulFire().instances()
      .values()
      .stream()
      .filter(instance -> context.getSource().instanceIds() == null || context.getSource().instanceIds()
        .stream()
        .anyMatch(instance.id()::equals))
      .toList();
  }

  public static List<BotConnection> getVisibleBots(InstanceManager instance, CommandContext<CommandSourceStack> context) {
    return instance.botConnections()
      .values()
      .stream()
      .filter(bot -> context.getSource().botIds() == null || context.getSource().botIds()
        .stream()
        .anyMatch(bot.accountProfileId()::equals))
      .toList();
  }

  public static List<BotConnection> getVisibleBots(CommandContext<CommandSourceStack> context) {
    return getVisibleInstances(context)
      .stream()
      .flatMap(instance -> getVisibleBots(instance, context).stream())
      .toList();
  }

  public static int forEveryInstance(
    CommandContext<CommandSourceStack> context,
    CommandFunction<InstanceManager> consumer) throws CommandSyntaxException {
    var instances = getVisibleInstances(context);
    if (instances.isEmpty()) {
      context.getSource().source().sendWarn("No instances found!");
      return 0;
    }

    var resultSum = 0;
    for (var instance : instances) {
      context.getSource().source().sendInfo("--- Running command for instance %s ---".formatted(instance.friendlyNameCache().get()));

      resultSum += consumer.run(instance);
    }

    return resultSum;
  }

  public static int forEveryInstanceEnsureHasBots(
    CommandContext<CommandSourceStack> context,
    CommandFunction<InstanceManager> consumer) throws CommandSyntaxException {
    return forEveryInstance(
      context,
      instance -> {
        if (getVisibleBots(instance, context).isEmpty()) {
          context.getSource().source().sendWarn("Instance %s has no connected bots!".formatted(instance.friendlyNameCache().get()));
          return 0;
        }

        return consumer.run(instance);
      });
  }

  public static int forEveryBot(
    CommandContext<CommandSourceStack> context,
    CommandFunction<BotConnection> consumer) throws CommandSyntaxException {
    return forEveryInstanceEnsureHasBots(
      context,
      instance -> {
        var resultSum = 0;
        for (var bot : getVisibleBots(instance, context)) {
          context.getSource().source().sendInfo("--- Running command for bot %s ---".formatted(bot.accountName()));
          resultSum += consumer.run(bot);
        }

        return resultSum;
      });
  }

  public static Component toComponent(Message message) {
    return switch (message) {
      case null -> null;
      case BrigadierComponent brigadierComponent -> brigadierComponent.component();
      default -> Component.text(message.getString());
    };
  }

  public interface CommandFunction<S> {
    int run(S subject) throws CommandSyntaxException;
  }

  public sealed interface HelpCarrier permits SingleRedirectHelpWrapper, CommandHelpWrapper, RedirectHelpWrapper {
    String help();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean privateCommand();
  }

  public record HelpData(String command, String help) {}

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
