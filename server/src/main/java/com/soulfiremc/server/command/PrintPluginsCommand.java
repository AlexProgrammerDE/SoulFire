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
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.SoulFireAPI;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.literal;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.privateCommand;

public class PrintPluginsCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("print-plugins")
        .executes(
          privateCommand(
            c -> {
              var builder = new StringBuilder("\n");
              for (var plugin : SoulFireAPI.getServerExtensions()) {
                if (!(plugin instanceof InternalPlugin)) {
                  continue;
                }

                var pluginInfo = plugin.pluginInfo();
                builder.append("| `%s` | %s | %s | %s |\n".formatted(pluginInfo.id(), pluginInfo.description(), pluginInfo.author(), pluginInfo.license()));
              }
              c.getSource().source().sendInfo(builder.toString());

              return Command.SINGLE_SUCCESS;
            })));
  }
}
