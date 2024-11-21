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
package com.soulfiremc.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.soulfiremc.brigadier.CommandHelpWrapper;
import com.soulfiremc.brigadier.PlatformCommandManager;
import com.soulfiremc.brigadier.RedirectHelpWrapper;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.api.event.lifecycle.CommandManagerInitEvent;
import com.soulfiremc.server.brigadier.*;
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.grpc.ServerRPCConstants;
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
import com.soulfiremc.server.spark.SFSparkCommandSender;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.ServerCommandSource;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.UUIDHelper;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.raphimc.vialoader.util.ProtocolVersionList;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static com.soulfiremc.server.brigadier.ServerBrigadierHelper.*;

/**
 * Holds and configures all server-side text commands of SoulFire itself.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager implements PlatformCommandManager<ServerCommandSource> {
  private static final ThreadLocal<Map<String, String>> COMMAND_CONTEXT =
    ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);
  @Getter
  private final CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();
  private final SoulFireServer soulFireServer;

  private static String getCurrentUsername() {
    var currentUser = ServerRPCConstants.USER_CONTEXT_KEY.get();

    if (currentUser == null) {
      return "Console";
    } else {
      return currentUser.getUsername();
    }
  }

  @PostConstruct
  public void postConstruct() {
    // Help
    dispatcher.register(
      literal("help")
        .executes(
          help(
            "Prints a list of all available commands",
            c -> {
              c.getSource().sendInfo("Available commands:");
              for (var command : getAllUsage(dispatcher.getRoot(), c.getSource())) {
                c.getSource().sendInfo("{} -> {}", command.command(), command.help());
              }

              return Command.SINGLE_SUCCESS;
            })));

    // Administration
    dispatcher.register(
      literal("generate-token")
        .executes(
          help(
            "Generate a JWT for other clients to connect to the server",
            c -> {
              c.getSource()
                .sendInfo(
                  "JWT (This gives full SF access, make sure you only give this to trusted users): {}",
                  soulFireServer.generateRemoteUserJWT());

              return Command.SINGLE_SUCCESS;
            })));
    dispatcher.register(
      literal("whoami")
        .executes(
          help(
            "See who you are",
            c -> {
              c.getSource().sendInfo("Username: {}", getCurrentUsername());

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
                c.getSource().sendWarn("No plugins found!");
                return Command.SINGLE_SUCCESS;
              }

              extensions.forEach(
                plugin -> {
                  var pluginInfo = plugin.pluginInfo();
                  c.getSource()
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
                  return executePathfinding(c, bot -> new XZGoal(xz.getAbsoluteLocation(Vector2d.from(
                    bot.dataManager().localPlayer().x(),
                    bot.dataManager().localPlayer().z()
                  )).toInt()));
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
                      if (entityId == -1) {
                        c.getSource().sendWarn("Invalid entity specified!");
                        return Command.SINGLE_SUCCESS;
                      }

                      bot.scheduler().schedule(() -> new FollowEntityController(
                        entityId,
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
      literal("stop-path")
        .executes(
          help(
            "Makes selected bots stop pathfinding",
            c ->
              forEveryBot(
                c,
                bot -> {
                  var changed = EventUtil.runAndCountChanged(
                    SoulFireAPI.getEventManager(),
                    () ->
                      SoulFireAPI.getEventManager()
                        .unregisterAll(
                          BotPreTickEvent.class,
                          (clazz, o) -> {
                            if (PathExecutor.class.isAssignableFrom(clazz)) {
                              ((PathExecutor) o.orElseThrow()).cancel();
                              return true;
                            }

                            return false;
                          }));

                  bot.controlState().resetAll();

                  if (changed == 0) {
                    c.getSource().sendWarn("No pathfinding was running!");
                  } else {
                    c.getSource()
                      .sendInfo("Stopped pathfinding for " + bot.accountName());
                  }
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
                    bot.dataManager()
                      .localPlayer()
                      .lookAt(
                        RotationOrigin.EYES,
                        xyz.getAbsoluteLocation(bot.dataManager().localPlayer().pos()));
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
                      var controlState = bot.controlState();

                      controlState.forward(!controlState.forward());
                      controlState.backward(false);
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
                      var controlState = bot.controlState();

                      controlState.backward(!controlState.backward());
                      controlState.forward(false);
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
                      var controlState = bot.controlState();

                      controlState.left(!controlState.left());
                      controlState.right(false);
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
                      var controlState = bot.controlState();

                      controlState.right(!controlState.right());
                      controlState.left(false);
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
                  var controlState = bot.controlState();

                  controlState.jumping(!controlState.jumping());
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
                  bot.botControl().toggleSneak();
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
                  bot.controlState().resetAll();
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
                        bot.botActionManager().placeBlock(Hand.MAIN_HAND, block.getAbsoluteLocation(bot.dataManager().localPlayer().pos()).toInt(), face);
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
              forEveryAttack(
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
                  .executeCommand(new SFSparkCommandSender(c.getSource()), command.split(ARGUMENT_SEPARATOR));
                return Command.SINGLE_SUCCESS;
              })))
        .executes(
          help(
            "Get spark help",
            c -> {
              SFSparkPlugin.INSTANCE.platform().executeCommand(new SFSparkCommandSender(c.getSource()), new String[]{});
              return Command.SINGLE_SUCCESS;
            })));

    // Utility commands
    dispatcher.register(
      literal("online")
        .executes(
          help(
            "Shows connected bots in attacks",
            c ->
              forEveryAttackEnsureHasBots(
                c,
                instanceManager -> {
                  var online = new ArrayList<String>();
                  for (var bot : instanceManager.botConnections().values()) {
                    if (bot.isOnline()) {
                      online.add(bot.accountName());
                    }
                  }

                  c.getSource()
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
              forEveryAttackEnsureHasBots(
                c,
                instanceManager -> {
                  c.getSource()
                    .sendInfo(
                      "Total bots: {}", instanceManager.botConnections().size());
                  long readTraffic = 0;
                  long writeTraffic = 0;
                  for (var bot : instanceManager.botConnections().values()) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    readTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                    writeTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
                  }

                  c.getSource()
                    .sendInfo(
                      "Total read traffic: {}",
                      FileUtils.byteCountToDisplaySize(readTraffic));
                  c.getSource()
                    .sendInfo(
                      "Total write traffic: {}",
                      FileUtils.byteCountToDisplaySize(writeTraffic));

                  long currentReadTraffic = 0;
                  long currentWriteTraffic = 0;
                  for (var bot : instanceManager.botConnections().values()) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    currentReadTraffic +=
                      trafficShapingHandler.trafficCounter().lastReadThroughput();
                    currentWriteTraffic +=
                      trafficShapingHandler.trafficCounter().lastWriteThroughput();
                  }

                  c.getSource()
                    .sendInfo(
                      "Current read traffic: {}/s",
                      FileUtils.byteCountToDisplaySize(currentReadTraffic));
                  c.getSource()
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
                    if (entityId == -1) {
                      c.getSource().sendWarn("Invalid entity specified!");
                      return Command.SINGLE_SUCCESS;
                    }

                    var entity = bot.dataManager().entityTrackerState().getEntity(entityId);
                    if (entity == null) {
                      c.getSource().sendWarn("Entity not found!");
                      return Command.SINGLE_SUCCESS;
                    }

                    c.getSource().sendInfo("Metadata for entity {}: {}", entityId, entity.metadataState().toNamedMap());

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
              c.getSource().sendInfo(builder.toString());

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
              c.getSource().sendInfo(builder.toString());

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
              c.getSource().sendInfo(builder.toString());

              return Command.SINGLE_SUCCESS;
            })));

    // Context commands
    dispatcher.register(
      literal("bot")
        .then(
          argument("bot_names", StringArgumentType.string())
            .suggests(
              (c, b) -> {
                forEveryBot(
                  c,
                  bot -> {
                    b.suggest(bot.accountName());

                    return Command.SINGLE_SUCCESS;
                  },
                  false);

                return b.buildFuture();
              })
            .forward(
              dispatcher.getRoot(),
              helpRedirect(
                "Instead of running a command for selected bots, run it for a specific list of bots. Use a comma to separate the names",
                c -> {
                  COMMAND_CONTEXT.get().put("bot_names", StringArgumentType.getString(c, "bot_names"));
                  return Collections.singleton(c.getSource());
                }),
              false)));
    dispatcher.register(
      literal("attack")
        .then(
          argument("attack_ids", StringArgumentType.string())
            .suggests(
              (c, b) -> {
                forEveryAttack(
                  c,
                  instanceManager -> {
                    b.suggest(instanceManager.id().toString());

                    return Command.SINGLE_SUCCESS;
                  },
                  false);

                return b.buildFuture();
              })
            .forward(
              dispatcher.getRoot(),
              helpRedirect(
                "Instead of running a command for selected attacks, run it for a specific list of attacks. Use a comma to separate the ids",
                c -> {
                  COMMAND_CONTEXT.get().put("attack_ids", StringArgumentType.getString(c, "attack_ids"));
                  return Collections.singleton(c.getSource());
                }),
              false)));

    SoulFireAPI.postEvent(new CommandManagerInitEvent(soulFireServer, this));
  }

  private int exportMap(
    CommandContext<ServerCommandSource> context, Function<BotConnection, IntSet> idProvider) {
    // Inside here to capture a time for the file name
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        for (var mapId : idProvider.apply(bot).toIntArray()) {
          var mapDataState = bot.dataManager().mapDataStates().get(mapId);
          if (mapDataState == null) {
            context.getSource().sendInfo("Map not found!");
            return Command.SINGLE_SUCCESS;
          }

          var image = mapDataState.toBufferedImage();
          var fileName = "map_%d_%d_%s.png".formatted(currentTime, mapId, bot.accountName());
          try {
            var mapsDirectory = SFPathConstants.getMapsDirectory(soulFireServer.baseDirectory());
            Files.createDirectories(mapsDirectory);
            var file = mapsDirectory.resolve(fileName);
            ImageIO.write(image, "png", file.toFile());
            context.getSource().sendInfo("Exported map to {}", file);
          } catch (IOException e) {
            context.getSource().sendError("Failed to export map!", e);
          }
        }

        return Command.SINGLE_SUCCESS;
      });
  }

  public int forEveryAttack(
    CommandContext<ServerCommandSource> context, ToIntFunction<InstanceManager> consumer) {
    return forEveryAttack(context, consumer, true);
  }

  private int forEveryAttack(
    CommandContext<ServerCommandSource> context,
    ToIntFunction<InstanceManager> consumer,
    boolean printMessages) {
    if (soulFireServer.instances().isEmpty()) {
      if (printMessages) {
        context.getSource().sendWarn("No attacks found!");
      }

      return 2;
    }

    var resultCode = Command.SINGLE_SUCCESS;
    for (var instanceManager : soulFireServer.instances().values()) {
      if (COMMAND_CONTEXT.get().containsKey("attack_ids")
        && Arrays.stream(COMMAND_CONTEXT.get().get("attack_ids").split(","))
        .map(UUIDHelper::tryParseUniqueId)
        .filter(Optional::isPresent)
        .noneMatch(i -> i.orElseThrow().equals(instanceManager.id()))) {
        continue;
      }

      if (printMessages) {
        context
          .getSource()
          .sendInfo("--- Running command for attack " + instanceManager.id() + " ---");
      }

      var result = consumer.applyAsInt(instanceManager);
      if (result != Command.SINGLE_SUCCESS) {
        resultCode = result;
      }
    }

    return resultCode;
  }

  public int forEveryAttackEnsureHasBots(
    CommandContext<ServerCommandSource> context, ToIntFunction<InstanceManager> consumer) {
    return forEveryAttackEnsureHasBots(context, consumer, true);
  }

  private int forEveryAttackEnsureHasBots(
    CommandContext<ServerCommandSource> context,
    ToIntFunction<InstanceManager> consumer,
    boolean printMessages) {
    return forEveryAttack(
      context,
      instanceManager -> {
        if (instanceManager.botConnections().isEmpty()) {
          if (printMessages) {
            context.getSource().sendWarn("No bots found in attack " + instanceManager.id() + "!");
          }
          return 3;
        }

        return consumer.applyAsInt(instanceManager);
      },
      printMessages);
  }

  public int forEveryBot(
    CommandContext<ServerCommandSource> context, ToIntFunction<BotConnection> consumer) {
    return forEveryBot(context, consumer, true);
  }

  private int forEveryBot(
    CommandContext<ServerCommandSource> context,
    ToIntFunction<BotConnection> consumer,
    boolean printMessages) {
    return forEveryAttackEnsureHasBots(
      context,
      instanceManager -> {
        var resultCode = Command.SINGLE_SUCCESS;
        for (var bot : instanceManager.botConnections().values()) {
          if (COMMAND_CONTEXT.get().containsKey("bot_names")
            && Arrays.stream(COMMAND_CONTEXT.get().get("bot_names").split(","))
            .noneMatch(s -> s.equals(bot.accountName()))) {
            continue;
          }

          if (printMessages) {
            context
              .getSource()
              .sendInfo("--- Running command for bot " + bot.accountName() + " ---");
          }

          var result = consumer.applyAsInt(bot);
          if (result != Command.SINGLE_SUCCESS) {
            resultCode = result;
          }
        }

        return resultCode;
      },
      printMessages);
  }

  public int executePathfinding(CommandContext<ServerCommandSource> context,
                                Function<BotConnection, GoalScorer> goalScorerFactory) {
    return forEveryBot(
      context,
      bot -> {
        PathExecutor.executePathfinding(bot, goalScorerFactory.apply(bot), new PathConstraint(bot));
        return Command.SINGLE_SUCCESS;
      });
  }

  @Override
  public int execute(String command, ServerCommandSource source) {
    command = command.strip();

    try {
      return dispatcher.execute(command, source);
    } catch (CommandSyntaxException e) {
      source.sendWarn(e.getMessage());
      return Command.SINGLE_SUCCESS;
    } finally {
      COMMAND_CONTEXT.get().clear();
    }
  }

  @Override
  public List<String> getCompletionSuggestions(String command, ServerCommandSource source) {
    try {
      return dispatcher
        .getCompletionSuggestions(dispatcher.parse(command, source))
        .join()
        .getList()
        .stream()
        .map(Suggestion::getText)
        .toList();
    } finally {
      COMMAND_CONTEXT.get().clear();
    }
  }

  private HelpData[] getAllUsage(
    final CommandNode<ServerCommandSource> node, final ServerCommandSource source) {
    final var result = new ArrayList<HelpData>();
    getAllUsage(node, source, result, "");
    return result.toArray(new HelpData[0]);
  }

  private void getAllUsage(
    final CommandNode<ServerCommandSource> node,
    final ServerCommandSource source,
    final ArrayList<HelpData> result,
    final String prefix) {
    if (!node.canUse(source)) {
      return;
    }

    if (node.getCommand() != null) {
      var helpWrapper = (CommandHelpWrapper<?>) node.getCommand();
      if (!helpWrapper.privateCommand()) {
        result.add(new HelpData(prefix, helpWrapper.help()));
      }
    }

    if (node.getRedirect() != null) {
      var redirectHelpWrapper = (RedirectHelpWrapper<?>) node.getRedirectModifier();
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
}
