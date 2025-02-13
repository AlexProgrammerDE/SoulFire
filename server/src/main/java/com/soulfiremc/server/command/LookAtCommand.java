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
import com.soulfiremc.server.protocol.bot.ControllingTask;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class LookAtCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("lookat")
        .then(argument("xyz", new DynamicXYZArgumentType())
          .executes(
            help(
              "Makes selected bots look at the block at the xyz coordinates",
              c -> {
                var xyz = c.getArgument("xyz", DynamicXYZArgumentType.XYZLocationMapper.class);

                return forEveryBot(
                  c,
                  bot -> {
                    bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> bot.dataManager()
                      .localPlayer()
                      .lookAt(
                        RotationOrigin.EYES,
                        xyz.getAbsoluteLocation(bot.dataManager().localPlayer().pos()))));
                    return Command.SINGLE_SUCCESS;
                  });
              }))));
  }
}
