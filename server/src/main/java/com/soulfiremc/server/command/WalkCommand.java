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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.server.command.brigadier.DoubleAxisArgumentType;
import com.soulfiremc.server.command.brigadier.EntityArgumentType;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.goals.XZGoal;
import com.soulfiremc.server.pathfinding.goals.YGoal;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.protocol.BotConnection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2d;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class WalkCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("walk")
        .then(argument("entity", StringArgumentType.string())
          .executes(
            help(
              "Makes selected bots walk to an entity",
              c -> {
                var entityMatcher = EntityArgumentType.getEntityMatcher(c, "entity");

                return forEveryBot(
                  c,
                  bot -> {
                    var entity = bot.dataManager().currentLevel().entityTracker()
                      .getEntities()
                      .stream()
                      .filter(entityMatcher)
                      .findAny();
                    if (entity.isEmpty()) {
                      c.getSource().source().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    PathExecutor.executePathfinding(
                      bot,
                      new PosGoal(SFVec3i.fromDouble(entity.get().pos())),
                      new PathConstraint(bot)
                    );

                    return Command.SINGLE_SUCCESS;
                  });
              })))
        .then(literal("radius")
          .then(argument("radius", IntegerArgumentType.integer())
            .executes(
              help(
                "Makes selected bots walk to a random xz position within the radius",
                c -> {
                  var radius = IntegerArgumentType.getInteger(c, "radius");

                  return executePathfinding(c, bot -> {
                    var random = ThreadLocalRandom.current();
                    var pos = bot.dataManager().localPlayer().pos();
                    var x =
                      random.nextInt(
                        pos.getFloorX() - radius,
                        pos.getFloorX() + radius);
                    var z =
                      random.nextInt(
                        pos.getFloorZ() - radius,
                        pos.getFloorZ() + radius);

                    return new XZGoal(x, z);
                  });
                }))))
        .then(argument("y", DoubleAxisArgumentType.INSTANCE)
          .executes(
            help(
              "Makes selected bots walk to the y coordinates",
              c -> {
                var y = DoubleAxisArgumentType.getDoubleAxisData(c, "y");
                return executePathfinding(c, bot -> new YGoal(GenericMath.floor(
                  DoubleAxisArgumentType.forYAxis(y, bot.dataManager().localPlayer().y())
                )));
              })))
        .then(argument("x", DoubleAxisArgumentType.INSTANCE)
          .then(argument("z", DoubleAxisArgumentType.INSTANCE)
            .executes(
              help(
                "Makes selected bots walk to the xz coordinates",
                c -> {
                  var x = DoubleAxisArgumentType.getDoubleAxisData(c, "x");
                  var z = DoubleAxisArgumentType.getDoubleAxisData(c, "z");
                  return executePathfinding(c, bot -> {
                    var xzGoal = DoubleAxisArgumentType.forXZAxis(x, z, Vector2d.from(
                      bot.dataManager().localPlayer().x(),
                      bot.dataManager().localPlayer().z()
                    )).toInt();
                    return new XZGoal(xzGoal.getX(), xzGoal.getY());
                  });
                }))))
        .then(argument("x", DoubleAxisArgumentType.INSTANCE)
          .then(argument("y", DoubleAxisArgumentType.INSTANCE)
            .then(argument("z", DoubleAxisArgumentType.INSTANCE)
              .executes(
                help(
                  "Makes selected bots walk to the xyz coordinates",
                  c -> {
                    var x = DoubleAxisArgumentType.getDoubleAxisData(c, "x");
                    var y = DoubleAxisArgumentType.getDoubleAxisData(c, "y");
                    var z = DoubleAxisArgumentType.getDoubleAxisData(c, "z");
                    return executePathfinding(c, bot -> new PosGoal(SFVec3i.fromDouble(
                      DoubleAxisArgumentType.forXYZAxis(x, y, z, bot.dataManager().localPlayer().pos())
                    )));
                  }))))));
  }

  public static int executePathfinding(CommandContext<CommandSourceStack> context,
                                       Function<BotConnection, GoalScorer> goalScorerFactory) throws CommandSyntaxException {
    return forEveryBot(
      context,
      bot -> {
        PathExecutor.executePathfinding(bot, goalScorerFactory.apply(bot), new PathConstraint(bot));
        return Command.SINGLE_SUCCESS;
      });
  }
}
