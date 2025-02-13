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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.soulfiremc.server.InstanceManager;

import java.util.Set;
import java.util.stream.Collectors;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class InstanceCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("instance")
        .then(
          argument("instance_names", StringArgumentType.string())
            .suggests(
              (c, b) -> {
                getVisibleInstances(c).forEach(instance -> b.suggest(instance.friendlyNameCache().get()));

                return b.buildFuture();
              })
            .redirect(
              dispatcher.getRoot(),
              helpSingleRedirect(
                "Instead of running a command for all possible instances, run it for a specific list of instances. Use a comma to separate the names",
                c -> {
                  var instanceNames = Set.of(StringArgumentType.getString(c, "instance_names").split(","));
                  return c.getSource()
                    .withInstanceIds(getVisibleInstances(c)
                      .stream()
                      .filter(instance -> instanceNames.contains(instance.friendlyNameCache().get()))
                      .map(InstanceManager::id)
                      .collect(Collectors.toSet()));
                }
              ))));
  }
}
