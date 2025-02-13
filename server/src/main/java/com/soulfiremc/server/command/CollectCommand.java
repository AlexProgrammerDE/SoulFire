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
import com.soulfiremc.server.command.brigadier.BlockTagResolvable;
import com.soulfiremc.server.command.brigadier.TagBasedArgumentType;
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.controller.CollectBlockController;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class CollectCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("collect")
        .then(argument("block", new TagBasedArgumentType<BlockType, BlockTagResolvable>(
          key -> tags -> block -> block.key().equals(key),
          key -> tags -> block -> tags.is(block, key),
          BlockType.REGISTRY.values().stream().map(BlockType::key).toList(),
          BlockTags.TAGS
        ))
          .then(argument("amount", IntegerArgumentType.integer(1))
            .then(argument("searchRadius", IntegerArgumentType.integer(1))
              .executes(
                help(
                  "Makes selected bots collect a block by name or tag",
                  c -> {
                    var resolvable = c.getArgument("block", BlockTagResolvable.class);
                    var amount = IntegerArgumentType.getInteger(c, "amount");
                    var searchRadius = IntegerArgumentType.getInteger(c, "searchRadius");

                    return forEveryBot(
                      c,
                      bot -> {
                        bot.scheduler().schedule(() -> new CollectBlockController(
                          resolvable.resolve(bot.dataManager().tagsState()),
                          amount,
                          searchRadius
                        ).start(bot));

                        return Command.SINGLE_SUCCESS;
                      });
                  }))))));
  }
}
