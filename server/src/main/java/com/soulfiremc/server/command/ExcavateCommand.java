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
import com.soulfiremc.server.command.brigadier.DynamicXYZArgumentType;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.controller.ExcavateAreaController;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class ExcavateCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("excavate")
        .then(literal("rectangle")
          .then(argument("from", new DynamicXYZArgumentType())
            .then(argument("to", new DynamicXYZArgumentType())
              .executes(
                help(
                  "Makes selected bots dig a rectangle from the from to the to coordinates",
                  c -> {
                    var from = c.getArgument("from", DynamicXYZArgumentType.XYZLocationMapper.class);
                    var to = c.getArgument("to", DynamicXYZArgumentType.XYZLocationMapper.class);

                    return forEveryBot(
                      c,
                      bot -> {
                        var dataManager = bot.dataManager();
                        bot.scheduler().schedule(() -> new ExcavateAreaController(
                          ExcavateAreaController.getRectangleFromTo(
                            SFVec3i.fromDouble(from.getAbsoluteLocation(dataManager.localPlayer().pos())),
                            SFVec3i.fromDouble(to.getAbsoluteLocation(dataManager.localPlayer().pos()))
                          )
                        ).start(bot));

                        return Command.SINGLE_SUCCESS;
                      });
                  })))))
        .then(literal("sphere")
          .then(argument("position", new DynamicXYZArgumentType())
            .then(argument("radius", IntegerArgumentType.integer(1))
              .executes(
                help(
                  "Makes selected bots dig a sphere with the given radius",
                  c -> {
                    var position = c.getArgument("position", DynamicXYZArgumentType.XYZLocationMapper.class);
                    var radius = IntegerArgumentType.getInteger(c, "radius");

                    return forEveryBot(
                      c,
                      bot -> {
                        var dataManager = bot.dataManager();

                        bot.scheduler().schedule(() -> new ExcavateAreaController(
                          ExcavateAreaController.getSphereRadius(SFVec3i.fromDouble(position.getAbsoluteLocation(dataManager.localPlayer().pos())), radius)
                        ).start(bot));

                        return Command.SINGLE_SUCCESS;
                      });
                  }))))));
  }
}
