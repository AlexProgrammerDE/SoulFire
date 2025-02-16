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
import com.soulfiremc.server.command.brigadier.DynamicXYZArgumentType;
import com.soulfiremc.server.command.brigadier.EnumArgumentType;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class PlaceOnCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("placeon")
        .then(argument("block", new DynamicXYZArgumentType())
          .then(
            argument("face", new EnumArgumentType<>(BlockFace.class))
              .executes(
                help(
                  "Makes selected bots place a block on the specified face of a block",
                  c -> {
                    var block = c.getArgument("block", DynamicXYZArgumentType.XYZLocationMapper.class);
                    var face = c.getArgument("face", BlockFace.class);

                    return forEveryBot(
                      c,
                      bot -> {
                        bot.botControl().registerControllingTask(ControllingTask.singleTick(() ->
                          bot.dataManager().gameModeState().placeBlock(Hand.MAIN_HAND, block.getAbsoluteLocation(bot.dataManager().localPlayer().pos()).toInt(), face)));
                        return Command.SINGLE_SUCCESS;
                      });
                  })))));
  }
}
