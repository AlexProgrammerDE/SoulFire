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
import com.soulfiremc.server.command.brigadier.ArgumentTypeHelper;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class MetadataCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("metadata")
        .then(argument("entity", StringArgumentType.string())
          .executes(
            help(
              "Makes selected bots follow an entity by id",
              c -> {
                var entityName = StringArgumentType.getString(c, "entity");

                return forEveryBot(
                  c,
                  bot -> {
                    var entity = bot.dataManager().currentLevel().entityTracker()
                      .getEntities()
                      .stream()
                      .filter(ArgumentTypeHelper.parseEntityMatch(bot, entityName))
                      .findAny();
                    if (entity.isEmpty()) {
                      c.getSource().source().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    c.getSource().source().sendInfo("Metadata for entity {}: {}", entity.get().entityId(), entity.get().metadataState().toNamedMap());

                    return Command.SINGLE_SUCCESS;
                  });
              }))));
  }
}
