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
import com.mojang.brigadier.tree.CommandNode;
import com.soulfiremc.console.GenericTerminalConsole;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.lifecycle.CommandManagerInitEvent;
import com.soulfiremc.server.command.brigadier.*;
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.controller.CollectBlockController;
import com.soulfiremc.server.pathfinding.controller.ExcavateAreaController;
import com.soulfiremc.server.pathfinding.controller.FollowEntityController;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.goals.XZGoal;
import com.soulfiremc.server.pathfinding.goals.YGoal;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.spark.SFSparkCommandSender;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.SoulFireAdventure;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.viaversion.vialoader.util.ProtocolVersionList;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

/**
 * Holds and configures all server-side text commands of SoulFire itself.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager {
  @Getter
  private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
  private final SoulFireServer soulFireServer;

  @PostConstruct
  public void postConstruct() {
    // Help
    dispatcher.register(
      literal("help")
        .executes(
          help(
            "Prints a list of all available commands",
            c -> {
              c.getSource().source().sendInfo("Available commands:");
              for (var command : getAllUsage(dispatcher.getRoot(), c.getSource())) {
                c.getSource().source().sendInfo("{} -> {}", command.command(), command.help());
              }

              return Command.SINGLE_SUCCESS;
            })));

    // Administration
    dispatcher.register(
      literal("generate-token")
        .executes(
          help(
            "Generate an auth JWT for the current user",
            c -> {
              if (!(c.getSource().source() instanceof SoulFireUser user)) {
                c.getSource().source().sendInfo("Only SoulFire users can generate tokens.");
                return Command.SINGLE_SUCCESS;
              }

              var authSystem = soulFireServer.authSystem();
              user.sendInfo(
                "JWT (This gives full access to your user, make sure you only give this to trusted users): {}",
                authSystem.generateJWT(authSystem.getUserData(user.getUniqueId()).orElseThrow()));

              return Command.SINGLE_SUCCESS;
            })));
    dispatcher.register(
      literal("set-email")
        .then(argument("email", StringArgumentType.greedyString())
          .executes(
            help(
              "Set the email of the current user",
              c -> {
                if (!(c.getSource().source() instanceof SoulFireUser user)) {
                  c.getSource().source().sendInfo("Only SoulFire users can set their email.");
                  return Command.SINGLE_SUCCESS;
                }

                var email = StringArgumentType.getString(c, "email");
                soulFireServer.sessionFactory().inTransaction(s -> {
                  var userData = s.find(UserEntity.class, user.getUniqueId());
                  userData.email(email);
                  s.merge(userData);
                });
                c.getSource().source().sendInfo("Email of user {} set to {}", user.getUsername(), email);

                return Command.SINGLE_SUCCESS;
              }))));
    dispatcher.register(
      literal("whoami")
        .executes(
          help(
            "See who you are",
            c -> {
              c.getSource().source().sendInfo("Your are: {}",
                c.getSource().source() instanceof SoulFireUser user ? user.getUsername() : c.getSource().source().identifier());

              return Command.SINGLE_SUCCESS;
            })));
    dispatcher.register(
      literal("plugins")
        .executes(
          help(
            "Show all installed plugins",
            c -> {
              var extensions = SoulFireAPI.getServerExtensions();
              if (extensions.isEmpty()) {
                c.getSource().source().sendWarn("No plugins found!");
                return Command.SINGLE_SUCCESS;
              }

              extensions.forEach(
                plugin -> {
                  var pluginInfo = plugin.pluginInfo();
                  c.getSource().source()
                    .sendInfo(
                      "Plugin: {} | Version: {} | Description: {} | Author: {} | License: {}",
                      pluginInfo.id(),
                      pluginInfo.version(),
                      pluginInfo.description(),
                      pluginInfo.author(),
                      pluginInfo.license());
                });

              return Command.SINGLE_SUCCESS;
            })));

    // Pathfinding
    dispatcher.register(
      literal("walk")
        .then(argument("entity", StringArgumentType.string())
          .executes(
            help(
              "Makes selected bots walk to a entity",
              c -> {
                var entityName = StringArgumentType.getString(c, "entity");

                return forEveryBot(
                  c,
                  bot -> {
                    var entityId = ArgumentTypeHelper.parseEntityId(bot, entityName);
                    if (entityId.isEmpty()) {
                      c.getSource().source().sendWarn("Invalid entity specified!");
                      return Command.SINGLE_SUCCESS;
                    }

                    var entity = bot.dataManager().currentLevel().entityTracker().getEntity(entityId.getAsInt());
                    if (entity == null) {
                      c.getSource().source().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    PathExecutor.executePathfinding(
                      bot,
                      new PosGoal(SFVec3i.fromDouble(entity.pos())),
                      new PathConstraint(bot)
                    );

                    return Command.SINGLE_SUCCESS;
                  });
              })))
        .then(
          literal("radius")
            .then(
              argument("radius", IntegerArgumentType.integer())
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
        .then(
          argument("y", new DynamicYArgumentType())
            .executes(
              help(
                "Makes selected bots walk to the y coordinates",
                c -> {
                  var y = c.getArgument("y", DynamicYArgumentType.YLocationMapper.class);
                  return executePathfinding(c, bot -> new YGoal(GenericMath.floor(
                    y.getAbsoluteLocation(bot.dataManager().localPlayer().y())
                  )));
                })))
        .then(
          argument("xz", new DynamicXZArgumentType())
            .executes(
              help(
                "Makes selected bots walk to the xz coordinates",
                c -> {
                  var xz = c.getArgument("xz", DynamicXZArgumentType.XZLocationMapper.class);
                  return executePathfinding(c, bot -> {
                    var xzGoal = xz.getAbsoluteLocation(Vector2d.from(
                      bot.dataManager().localPlayer().x(),
                      bot.dataManager().localPlayer().z()
                    )).toInt();
                    return new XZGoal(xzGoal.getX(), xzGoal.getY());
                  });
                })))
        .then(
          argument("xyz", new DynamicXYZArgumentType())
            .executes(
              help(
                "Makes selected bots walk to the xyz coordinates",
                c -> {
                  var xyz = c.getArgument("xyz", DynamicXYZArgumentType.XYZLocationMapper.class);
                  return executePathfinding(c, bot -> new PosGoal(SFVec3i.fromDouble(
                    xyz.getAbsoluteLocation(bot.dataManager().localPlayer().pos())
                  )));
                }))));
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
    dispatcher.register(
      literal("follow")
        .then(argument("entity", StringArgumentType.string())
          .then(argument("maxRadius", IntegerArgumentType.integer(1))
            .executes(
              help(
                "Makes selected bots follow an entity by id",
                c -> {
                  var entityName = StringArgumentType.getString(c, "entity");
                  var maxRadius = IntegerArgumentType.getInteger(c, "maxRadius");

                  return forEveryBot(
                    c,
                    bot -> {
                      var entityId = ArgumentTypeHelper.parseEntityId(bot, entityName);
                      if (entityId.isEmpty()) {
                        c.getSource().source().sendWarn("Invalid entity specified!");
                        return Command.SINGLE_SUCCESS;
                      }

                      bot.scheduler().schedule(() -> new FollowEntityController(
                        entityId.getAsInt(),
                        maxRadius
                      ).start(bot));

                      return Command.SINGLE_SUCCESS;
                    });
                })))));
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
    dispatcher.register(
      literal("mimic")
        .then(argument("entity", StringArgumentType.string())
          .executes(
            help(
              "Makes selected bots mimic the movement of other entities",
              c -> {
                var entityName = StringArgumentType.getString(c, "entity");

                return forEveryBot(
                  c,
                  bot -> {
                    var entityId = ArgumentTypeHelper.parseEntityId(bot, entityName);
                    if (entityId.isEmpty()) {
                      c.getSource().source().sendWarn("Invalid entity specified!");
                      return Command.SINGLE_SUCCESS;
                    }

                    var entity = bot.dataManager().currentLevel().entityTracker().getEntity(entityId.getAsInt());
                    if (entity == null) {
                      c.getSource().source().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    var offset = entity.pos().sub(bot.dataManager().localPlayer().pos());
                    bot.botControl().registerControllingTask(new ControllingTask() {
                      @Override
                      public void tick() {
                        bot.controlState().resetAll();

                        var localPlayer = bot.dataManager().localPlayer();
                        localPlayer.setYRot(entity.yRot());
                        localPlayer.setXRot(entity.xRot());

                        localPlayer.setPos(entity.pos().sub(offset));
                        localPlayer.setDeltaMovement(entity.deltaMovement());
                      }

                      @Override
                      public void stop() {
                        bot.controlState().resetAll();
                      }

                      @Override
                      public boolean isDone() {
                        return false;
                      }
                    });

                    return Command.SINGLE_SUCCESS;
                  });
              }))));
    dispatcher.register(
      literal("stop-task")
        .executes(
          help(
            "Makes selected bots stop their current task",
            c ->
              forEveryBot(
                c,
                bot -> {
                  if (bot.botControl().stopControllingTask()) {
                    c.getSource().source().sendInfo("Stopped current task for " + bot.accountName());
                  } else {
                    c.getSource().source().sendWarn("No task was running!");
                  }

                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("start-attack")
        .executes(
          help(
            "Makes selected instances start an attack",
            c ->
              forEveryInstance(
                c,
                instance -> {
                  instance.switchToState(AttackLifecycle.RUNNING);

                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("pause-attack")
        .executes(
          help(
            "Makes selected instances pause their",
            c ->
              forEveryInstance(
                c,
                instance -> {
                  instance.switchToState(AttackLifecycle.PAUSED);

                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("stop-attack")
        .executes(
          help(
            "Makes selected instances stop their attack",
            c ->
              forEveryInstance(
                c,
                instance -> {
                  instance.switchToState(AttackLifecycle.STOPPED);

                  return Command.SINGLE_SUCCESS;
                }))));

    // Movement controls
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
    dispatcher.register(
      literal("move")
        .then(
          literal("forward")
            .executes(
              help(
                "Toggle walking forward for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.forward(!controlState.forward());
                        controlState.backward(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("backward")
            .executes(
              help(
                "Toggle walking backward for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.backward(!controlState.backward());
                        controlState.forward(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("left")
            .executes(
              help(
                "Toggle walking left for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.left(!controlState.left());
                        controlState.right(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("right")
            .executes(
              help(
                "Toggle walking right for selected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                        var controlState = bot.controlState();

                        controlState.right(!controlState.right());
                        controlState.left(false);
                      }));
                      return Command.SINGLE_SUCCESS;
                    })))));
    dispatcher.register(
      literal("jump")
        .executes(
          help(
            "Toggle jumping for selected bots",
            c ->
              forEveryBot(
                c,
                bot -> {
                  bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
                    var controlState = bot.controlState();

                    controlState.jumping(!controlState.jumping());
                  }));
                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("sneak")
        .executes(
          help(
            "Toggle sneaking for selected bots",
            c ->
              forEveryBot(
                c,
                bot -> {
                  bot.botControl().registerControllingTask(ControllingTask.singleTick(() ->
                    bot.botControl().toggleSneak()));
                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("reset")
        .executes(
          help(
            "Resets the movement of selected bots",
            c ->
              forEveryBot(
                c,
                bot -> {
                  bot.botControl().registerControllingTask(ControllingTask.singleTick(() ->
                    bot.controlState().resetAll()));
                  return Command.SINGLE_SUCCESS;
                }))));

    // Inventory controls
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
                          bot.botActionManager().placeBlock(Hand.MAIN_HAND, block.getAbsoluteLocation(bot.dataManager().localPlayer().pos()).toInt(), face)));
                        return Command.SINGLE_SUCCESS;
                      });
                  })))));

    // Attack controls
    dispatcher.register(
      literal("stop-attack")
        .executes(
          help(
            "Stops the ongoing attacks",
            c ->
              forEveryInstance(
                c,
                instanceManager -> {
                  instanceManager.stopAttackPermanently().join();
                  return Command.SINGLE_SUCCESS;
                }))));

    // Spark
    dispatcher.register(
      literal("spark")
        .then(argument("command", StringArgumentType.greedyString())
          .executes(
            help(
              "Runs a spark subcommand",
              c -> {
                var command = StringArgumentType.getString(c, "command");
                SFSparkPlugin.INSTANCE.platform()
                  .executeCommand(new SFSparkCommandSender(c.getSource().source()), command.split(ARGUMENT_SEPARATOR));
                return Command.SINGLE_SUCCESS;
              })))
        .executes(
          help(
            "Get spark help",
            c -> {
              SFSparkPlugin.INSTANCE.platform().executeCommand(new SFSparkCommandSender(c.getSource().source()), new String[]{});
              return Command.SINGLE_SUCCESS;
            })));

    // Utility commands
    dispatcher.register(
      literal("online")
        .executes(
          help(
            "Shows connected bots in attacks",
            c ->
              forEveryInstanceEnsureHasBots(
                c,
                instanceManager -> {
                  var online = new ArrayList<String>();
                  for (var bot : getVisibleBots(instanceManager, c)) {
                    if (bot.isOnline()) {
                      online.add(bot.accountName());
                    }
                  }

                  c.getSource().source()
                    .sendInfo(
                      online.size() + " bots online: " + String.join(", ", online));
                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("say")
        .then(
          argument("message", StringArgumentType.greedyString())
            .executes(
              help(
                "Makes selected bots send a message in chat or execute a command",
                c -> {
                  var message = StringArgumentType.getString(c, "message");

                  return forEveryBot(
                    c,
                    bot -> {
                      if (!bot.isOnline()) {
                        return Command.SINGLE_SUCCESS;
                      }

                      bot.botControl().sendMessage(message);

                      return Command.SINGLE_SUCCESS;
                    });
                }))));
    dispatcher.register(
      literal("stats")
        .executes(
          help(
            "Shows network stats",
            c ->
              forEveryInstanceEnsureHasBots(
                c,
                instanceManager -> {
                  var bots = getVisibleBots(instanceManager, c);
                  c.getSource().source()
                    .sendInfo(
                      "Total bots: {}", bots.size());
                  long readTraffic = 0;
                  long writeTraffic = 0;
                  for (var bot : bots) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    readTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                    writeTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
                  }

                  c.getSource().source()
                    .sendInfo(
                      "Total read traffic: {}",
                      FileUtils.byteCountToDisplaySize(readTraffic));
                  c.getSource().source()
                    .sendInfo(
                      "Total write traffic: {}",
                      FileUtils.byteCountToDisplaySize(writeTraffic));

                  long currentReadTraffic = 0;
                  long currentWriteTraffic = 0;
                  for (var bot : bots) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    currentReadTraffic +=
                      trafficShapingHandler.trafficCounter().lastReadThroughput();
                    currentWriteTraffic +=
                      trafficShapingHandler.trafficCounter().lastWriteThroughput();
                  }

                  c.getSource().source()
                    .sendInfo(
                      "Current read traffic: {}/s",
                      FileUtils.byteCountToDisplaySize(currentReadTraffic));
                  c.getSource().source()
                    .sendInfo(
                      "Current write traffic: {}/s",
                      FileUtils.byteCountToDisplaySize(currentWriteTraffic));

                  return Command.SINGLE_SUCCESS;
                }))));
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
                    var entityId = ArgumentTypeHelper.parseEntityId(bot, entityName);
                    if (entityId.isEmpty()) {
                      c.getSource().source().sendWarn("Invalid entity specified!");
                      return Command.SINGLE_SUCCESS;
                    }

                    var entity = bot.dataManager().currentLevel().entityTracker().getEntity(entityId.getAsInt());
                    if (entity == null) {
                      c.getSource().source().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    c.getSource().source().sendInfo("Metadata for entity {}: {}", entityId, entity.metadataState().toNamedMap());

                    return Command.SINGLE_SUCCESS;
                  });
              }))));
    dispatcher.register(
      literal("export-map")
        .executes(
          help(
            "Exports images of all map items. Can be a held item or in a item-frame.",
            c -> exportMap(c, bot -> bot.dataManager().mapDataStates().keySet())))
        .then(
          argument("map_id", IntegerArgumentType.integer())
            .executes(
              help(
                "Exports a image of a map item by map id. Can be a held item or in a item-frame.",
                c -> {
                  var mapId = IntegerArgumentType.getInteger(c, "map_id");
                  return exportMap(c, bot -> IntSet.of(mapId));
                }))));
    dispatcher.register(
      literal("print-versions")
        .executes(
          privateCommand(
            c -> {
              var builder = new StringBuilder("\n");
              ProtocolVersionList.getProtocolsNewToOld()
                .forEach(
                  version -> {
                    var versionId = "%s\\|%d".formatted(version.getVersionType().name(), version.getOriginalVersion());
                    String type;
                    if (SFVersionConstants.isBedrock(version)) {
                      type = "BEDROCK";
                    } else if (SFVersionConstants.isLegacy(version)) {
                      type = "LEGACY";
                    } else if (SFVersionConstants.isAprilFools(version)) {
                      type = "SNAPSHOT";
                    } else {
                      type = "JAVA";
                    }

                    builder.append(
                      "| `%s`%s | `%s` | `%s` |\n".formatted(version.getName(), SFVersionConstants.CURRENT_PROTOCOL_VERSION == version
                        ? " (native)" : "", versionId, type));
                  });
              c.getSource().source().sendInfo(builder.toString());

              return Command.SINGLE_SUCCESS;
            })));
    dispatcher.register(
      literal("print-commands")
        .executes(
          privateCommand(
            c -> {
              var builder = new StringBuilder("\n");
              for (var command : getAllUsage(dispatcher.getRoot(), c.getSource())) {
                builder.append("| `%s{:bash}` | %s |\n".formatted(command.command(), command.help()));
              }
              c.getSource().source().sendInfo(builder.toString());

              return Command.SINGLE_SUCCESS;
            })));
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

    // Context commands
    dispatcher.register(
      literal("bot")
        .then(
          argument("bot_names", StringArgumentType.string())
            .suggests(
              (c, b) -> {
                getVisibleBots(c).forEach(bot -> b.suggest(bot.accountName()));

                return b.buildFuture();
              })
            .redirect(
              dispatcher.getRoot(),
              helpSingleRedirect(
                "Instead of running a command for all possible bots, run it for a specific list of bots. Use a comma to separate the names",
                c -> {
                  var botNames = Set.of(StringArgumentType.getString(c, "bot_names").split(","));
                  return c.getSource()
                    .withBotIds(getVisibleBots(c)
                      .stream()
                      .filter(bot -> botNames.contains(bot.accountName()))
                      .map(BotConnection::accountProfileId)
                      .collect(Collectors.toSet()));
                }
              )
            )));
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
    dispatcher.register(
      literal("repeat")
        .then(
          argument("amount", IntegerArgumentType.integer(1))
            .fork(
              dispatcher.getRoot(),
              helpRedirect(
                "Repeat the command for the specified amount of times",
                c -> {
                  var amount = IntegerArgumentType.getInteger(c, "amount");
                  var list = new ArrayList<CommandSourceStack>();
                  for (var i = 0; i < amount; i++) {
                    list.add(c.getSource());
                  }

                  return list;
                })
            )));

    SoulFireAPI.postEvent(new CommandManagerInitEvent(soulFireServer, this));
  }

  private int exportMap(
    CommandContext<CommandSourceStack> context, Function<BotConnection, IntSet> idProvider) throws CommandSyntaxException {
    // Inside here to capture a time for the file name
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        for (var mapId : idProvider.apply(bot).toIntArray()) {
          var mapDataState = bot.dataManager().mapDataStates().get(mapId);
          if (mapDataState == null) {
            context.getSource().source().sendInfo("Map not found!");
            return Command.SINGLE_SUCCESS;
          }

          var image = mapDataState.toBufferedImage();
          var fileName = "map_%d_%d_%s.png".formatted(currentTime, mapId, bot.accountName());
          try {
            var mapsDirectory = SFPathConstants.getMapsDirectory(bot.instanceManager().getObjectStoragePath());
            Files.createDirectories(mapsDirectory);
            var file = mapsDirectory.resolve(fileName);
            ImageIO.write(image, "png", file.toFile());
            context.getSource().source().sendInfo("Exported map to {}", file);
          } catch (IOException e) {
            context.getSource().source().sendError("Failed to export map!", e);
          }
        }

        return Command.SINGLE_SUCCESS;
      });
  }

  public List<InstanceManager> getVisibleInstances(CommandContext<CommandSourceStack> context) {
    return soulFireServer.instances()
      .values()
      .stream()
      .filter(instance -> context.getSource().instanceIds() == null || context.getSource().instanceIds()
        .stream()
        .anyMatch(instance.id()::equals))
      .toList();
  }

  public List<BotConnection> getVisibleBots(InstanceManager instance, CommandContext<CommandSourceStack> context) {
    return instance.botConnections()
      .values()
      .stream()
      .filter(bot -> context.getSource().botIds() == null || context.getSource().botIds()
        .stream()
        .anyMatch(bot.accountProfileId()::equals))
      .toList();
  }

  public List<BotConnection> getVisibleBots(CommandContext<CommandSourceStack> context) {
    return getVisibleInstances(context)
      .stream()
      .flatMap(instance -> getVisibleBots(instance, context).stream())
      .toList();
  }

  private int forEveryInstance(
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

  private int forEveryInstanceEnsureHasBots(
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

  private int forEveryBot(
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

  public int executePathfinding(CommandContext<CommandSourceStack> context,
                                Function<BotConnection, GoalScorer> goalScorerFactory) throws CommandSyntaxException {
    return forEveryBot(
      context,
      bot -> {
        PathExecutor.executePathfinding(bot, goalScorerFactory.apply(bot), new PathConstraint(bot));
        return Command.SINGLE_SUCCESS;
      });
  }

  public int execute(String command, CommandSourceStack source) {
    command = command.strip();

    try {
      return dispatcher.execute(command, source);
    } catch (CommandSyntaxException e) {
      source.source().sendWarn(e.getMessage());
      return 0;
    }
  }

  public List<GenericTerminalConsole.Completion> complete(String command, int cursor, CommandSourceStack source) {
    return dispatcher
      .getCompletionSuggestions(dispatcher.parse(command, source), cursor)
      .join()
      .getList()
      .stream()
      .map(suggestion -> new GenericTerminalConsole.Completion(suggestion.getText(),
        SoulFireAdventure.TRUE_COLOR_ANSI_SERIALIZER.serializeOrNull(toComponent(suggestion.getTooltip()))))
      .toList();
  }

  private HelpData[] getAllUsage(
    final CommandNode<CommandSourceStack> node, final CommandSourceStack source) {
    final var result = new ArrayList<HelpData>();
    getAllUsage(node, source, result, "");
    return result.toArray(new HelpData[0]);
  }

  private void getAllUsage(
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
          child,
          source,
          result,
          prefix.isEmpty()
            ? child.getUsageText()
            : prefix + ARGUMENT_SEPARATOR + child.getUsageText());
      }
    }
  }

  private record HelpData(String command, String help) {}

  public interface CommandFunction<S> {
    int run(S subject) throws CommandSyntaxException;
  }
}
