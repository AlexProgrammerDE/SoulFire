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

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static com.soulfiremc.brigadier.BrigadierHelper.argument;
import static com.soulfiremc.brigadier.BrigadierHelper.help;
import static com.soulfiremc.brigadier.BrigadierHelper.helpRedirect;
import static com.soulfiremc.brigadier.BrigadierHelper.literal;
import static com.soulfiremc.brigadier.BrigadierHelper.privateCommand;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.soulfiremc.brigadier.CommandHelpWrapper;
import com.soulfiremc.brigadier.ConsoleSubject;
import com.soulfiremc.brigadier.PlatformCommandManager;
import com.soulfiremc.brigadier.RedirectHelpWrapper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.api.event.lifecycle.CommandManagerInitEvent;
import com.soulfiremc.server.brigadier.BlockTagResolvable;
import com.soulfiremc.server.brigadier.TagBasedArgumentType;
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.grpc.ServerRPCConstants;
import com.soulfiremc.server.pathfinding.controller.CollectBlockController;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.goals.XZGoal;
import com.soulfiremc.server.pathfinding.goals.YGoal;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.util.SFPathConstants;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.vialoader.util.ProtocolVersionList;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.vector.Vector3d;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager implements PlatformCommandManager {
  private static final Path HISTORY_FILE = SFPathConstants.DATA_FOLDER.resolve(".command_history");
  @Getter
  private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
  private final SoulFireServer soulFireServer;
  private final List<Map.Entry<Instant, String>> commandHistory =
    Collections.synchronizedList(new ArrayList<>());

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
    loadCommandHistory();

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
    dispatcher.register(
      literal("help-markdown")
        .executes(
          privateCommand(
            c -> {
              for (var command : getAllUsage(dispatcher.getRoot(), c.getSource())) {
                c.getSource()
                  .sendInfo(
                    String.format("| `%s` | %s |", command.command(), command.help()));
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

    // History commands
    dispatcher.register(
      literal("reload-history")
        .executes(
          help(
            "Refreshes the loaded command history from the data file",
            c -> {
              loadCommandHistory();

              return Command.SINGLE_SUCCESS;
            })));
    dispatcher.register(
      literal("clear-history")
        .executes(
          help(
            "Wipes the command history data",
            c -> {
              clearCommandHistory();

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
                    "Makes all connected bots walk to a random xz position within the radius",
                    c -> {
                      var radius = IntegerArgumentType.getInteger(c, "radius");

                      return forEveryBot(
                        c,
                        bot -> {
                          var random = ThreadLocalRandom.current();
                          var pos = bot.sessionDataManager().clientEntity().pos();
                          var x =
                            random.nextInt(
                              pos.getFloorX() - radius,
                              pos.getFloorX() + radius);
                          var z =
                            random.nextInt(
                              pos.getFloorZ() - radius,
                              pos.getFloorZ() + radius);

                          executePathfinding(bot, new XZGoal(x, z));

                          return Command.SINGLE_SUCCESS;
                        });
                    }))))
        .then(
          argument("y", IntegerArgumentType.integer())
            .executes(
              help(
                "Makes all connected bots walk to the y coordinates",
                c -> {
                  var y = IntegerArgumentType.getInteger(c, "y");
                  return executePathfinding(c, new YGoal(y));
                })))
        .then(
          argument("x", IntegerArgumentType.integer())
            .then(
              argument("z", IntegerArgumentType.integer())
                .executes(
                  help(
                    "Makes all connected bots walk to the xz coordinates",
                    c -> {
                      var x = IntegerArgumentType.getInteger(c, "x");
                      var z = IntegerArgumentType.getInteger(c, "z");

                      return executePathfinding(c, new XZGoal(x, z));
                    }))))
        .then(
          argument("x", IntegerArgumentType.integer())
            .then(
              argument("y", IntegerArgumentType.integer())
                .then(
                  argument("z", IntegerArgumentType.integer())
                    .executes(
                      help(
                        "Makes all connected bots walk to the xyz coordinates",
                        c -> {
                          var x = IntegerArgumentType.getInteger(c, "x");
                          var y = IntegerArgumentType.getInteger(c, "y");
                          var z = IntegerArgumentType.getInteger(c, "z");

                          return executePathfinding(c, new PosGoal(x, y, z));
                        }))))));
    dispatcher.register(
      literal("collect")
        .then(argument("block", new TagBasedArgumentType<BlockTagResolvable>(
          key -> tags -> block -> block.key().equals(key),
          key -> tags -> block -> tags.isBlockInTag(block, key),
          BlockType.FROM_KEY.keySet().stream().toList(),
          BlockTags.TAGS
        ))
          .then(argument("amount", IntegerArgumentType.integer(1))
            .then(argument("searchRadius", IntegerArgumentType.integer(1))
              .executes(
                help(
                  "Makes all connected bots collect a block by name or tag",
                  c -> {
                    var resolvable = c.getArgument("block", BlockTagResolvable.class);
                    var amount = IntegerArgumentType.getInteger(c, "amount");
                    var searchRadius = IntegerArgumentType.getInteger(c, "searchRadius");

                    return forEveryBot(
                      c,
                      bot -> {
                        bot.executorManager().newExecutorService(bot, "Pathfinding")
                          .submit(() -> {
                            new CollectBlockController(
                              resolvable.resolve(bot.sessionDataManager().tagsState()),
                              amount,
                              searchRadius
                            ).start(bot);
                          });

                        return Command.SINGLE_SUCCESS;
                      });
                  }))))));
    dispatcher.register(
      literal("stop-path")
        .executes(
          help(
            "Makes all connected bots stop pathfinding",
            c ->
              forEveryBot(
                c,
                bot -> {
                  var changed = EventUtil.runAndCountChanged(
                    bot.eventBus(),
                    () ->
                      bot.eventBus()
                        .unregisterAll(
                          BotPreTickEvent.class,
                          (clazz, o) -> {
                            if (PathExecutor.class.isAssignableFrom(clazz)) {
                              ((PathExecutor) o.orElseThrow()).cancel();
                              return true;
                            }

                            return false;
                          }));

                  bot.sessionDataManager().controlState().resetAll();

                  if (changed == 0) {
                    c.getSource().sendWarn("No pathfinding was running!");
                  } else {
                    c.getSource()
                      .sendInfo("Stopped pathfinding for " + bot.meta().accountName());
                  }
                  return Command.SINGLE_SUCCESS;
                }))));

    // Movement controls
    dispatcher.register(
      literal("lookat")
        .then(
          argument("x", DoubleArgumentType.doubleArg())
            .then(
              argument("y", DoubleArgumentType.doubleArg())
                .then(
                  argument("z", DoubleArgumentType.doubleArg())
                    .executes(
                      help(
                        "Makes all connected bots look at the block at the xyz coordinates",
                        c -> {
                          var x = DoubleArgumentType.getDouble(c, "x");
                          var y = DoubleArgumentType.getDouble(c, "y");
                          var z = DoubleArgumentType.getDouble(c, "z");

                          return forEveryBot(
                            c,
                            bot -> {
                              bot.sessionDataManager()
                                .clientEntity()
                                .lookAt(
                                  RotationOrigin.EYES,
                                  Vector3d.from(x, y, z));
                              return Command.SINGLE_SUCCESS;
                            });
                        }))))));
    dispatcher.register(
      literal("move")
        .then(
          literal("forward")
            .executes(
              help(
                "Toggle walking forward for all connected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var controlState = bot.sessionDataManager().controlState();

                      controlState.forward(!controlState.forward());
                      controlState.backward(false);
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("backward")
            .executes(
              help(
                "Toggle walking backward for all connected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var controlState = bot.sessionDataManager().controlState();

                      controlState.backward(!controlState.backward());
                      controlState.forward(false);
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("left")
            .executes(
              help(
                "Toggle walking left for all connected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var controlState = bot.sessionDataManager().controlState();

                      controlState.left(!controlState.left());
                      controlState.right(false);
                      return Command.SINGLE_SUCCESS;
                    }))))
        .then(
          literal("right")
            .executes(
              help(
                "Toggle walking right for all connected bots",
                c ->
                  forEveryBot(
                    c,
                    bot -> {
                      var controlState = bot.sessionDataManager().controlState();

                      controlState.right(!controlState.right());
                      controlState.left(false);
                      return Command.SINGLE_SUCCESS;
                    })))));
    dispatcher.register(
      literal("jump")
        .executes(
          help(
            "Toggle jumping for all connected bots",
            c ->
              forEveryBot(
                c,
                bot -> {
                  var controlState = bot.sessionDataManager().controlState();

                  controlState.jumping(!controlState.jumping());
                  return Command.SINGLE_SUCCESS;
                }))));
    dispatcher.register(
      literal("sneak")
        .executes(
          help(
            "Toggle sneaking for all connected bots",
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
            "Resets the movement of all connected bots",
            c ->
              forEveryBot(
                c,
                bot -> {
                  bot.sessionDataManager().controlState().resetAll();
                  return Command.SINGLE_SUCCESS;
                }))));

    // Attack controls
    dispatcher.register(
      literal("stop-attack")
        .executes(
          help(
            "Stops the ongoing attacks",
            c ->
              forEveryAttack(
                c,
                attackManager -> {
                  soulFireServer.stopAttack(attackManager.id());
                  return Command.SINGLE_SUCCESS;
                }))));

    // Utility commands
    dispatcher.register(
      literal("online")
        .executes(
          help(
            "Shows connected bots in attacks",
            c ->
              forEveryAttackEnsureHasBots(
                c,
                attackManager -> {
                  var online = new ArrayList<String>();
                  for (var bot : attackManager.botConnections().values()) {
                    if (bot.isOnline()) {
                      online.add(bot.meta().accountName());
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
                "Makes all connected bots send a message in chat or execute a command",
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
                attackManager -> {
                  c.getSource()
                    .sendInfo(
                      "Total bots: {}", attackManager.botConnections().size());
                  long readTraffic = 0;
                  long writeTraffic = 0;
                  for (var bot : attackManager.botConnections().values()) {
                    var trafficShapingHandler = bot.getTrafficHandler();

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
                  for (var bot : attackManager.botConnections().values()) {
                    var trafficShapingHandler = bot.getTrafficHandler();

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
      literal("export-map")
        .executes(
          help(
            "Exports images of all map items. Can be a held item or in a item-frame.",
            c -> exportMap(c, bot -> bot.sessionDataManager().mapDataStates().keySet())))
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
      literal("generate-versions")
        .executes(
          help(
            "Create a table of all supported protocol versions",
            c -> {
              var yesEmoji = "✅";
              var noEmoji = "❌";

              var builder = new StringBuilder("\n");
              ProtocolVersionList.getProtocolsNewToOld()
                .forEach(
                  version -> {
                    var nativeVersion =
                      SFVersionConstants.CURRENT_PROTOCOL_VERSION == version
                        ? yesEmoji
                        : noEmoji;
                    var bedrockVersion =
                      SFVersionConstants.isBedrock(version) ? yesEmoji : noEmoji;
                    var javaVersion =
                      !SFVersionConstants.isBedrock(version) ? yesEmoji : noEmoji;
                    var snapshotVersion =
                      SFVersionConstants.isAprilFools(version) ? yesEmoji : noEmoji;
                    var legacyVersion =
                      SFVersionConstants.isLegacy(version) ? yesEmoji : noEmoji;

                    builder.append(
                      String.format(
                        "| %s | %s | %s | %s | %s | %s |\n",
                        version.getName(),
                        nativeVersion,
                        javaVersion,
                        snapshotVersion,
                        legacyVersion,
                        bedrockVersion));
                  });
              log.info(builder.toString());

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
                    b.suggest(bot.meta().accountName());

                    return Command.SINGLE_SUCCESS;
                  },
                  false);

                return b.buildFuture();
              })
            .forward(
              dispatcher.getRoot(),
              helpRedirect(
                "Instead of running a command for all bots, run it for a specific list of bots. Use a comma to separate the names",
                c -> {
                  c.getSource()
                    .extraData
                    .put("bot_names", StringArgumentType.getString(c, "bot_names"));
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
                  attackManager -> {
                    b.suggest(attackManager.id());

                    return Command.SINGLE_SUCCESS;
                  },
                  false);

                return b.buildFuture();
              })
            .forward(
              dispatcher.getRoot(),
              helpRedirect(
                "Instead of running a command for all attacks, run it for a specific list of attacks. Use a comma to separate the ids",
                c -> {
                  c.getSource()
                    .extraData
                    .put("attack_ids", StringArgumentType.getString(c, "attack_ids"));
                  return Collections.singleton(c.getSource());
                }),
              false)));

    SoulFireAPI.postEvent(new CommandManagerInitEvent(this));
  }

  private int exportMap(
    CommandContext<ConsoleSubject> context, Function<BotConnection, IntSet> idProvider) {
    // Inside here to capture a time for the file name
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        for (var mapId : idProvider.apply(bot).toIntArray()) {
          var mapDataState = bot.sessionDataManager().mapDataStates().get(mapId);
          if (mapDataState == null) {
            context.getSource().sendInfo("Map not found!");
            return Command.SINGLE_SUCCESS;
          }

          var image = mapDataState.toBufferedImage();
          var fileName =
            "map_" + currentTime + "_" + mapId + "_" + bot.meta().accountName() + ".png";
          try {
            Files.createDirectories(SFPathConstants.MAPS_FOLDER);
            var file = SFPathConstants.MAPS_FOLDER.resolve(fileName);
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
    CommandContext<ConsoleSubject> context, ToIntFunction<AttackManager> consumer) {
    return forEveryAttack(context, consumer, true);
  }

  private int forEveryAttack(
    CommandContext<ConsoleSubject> context,
    ToIntFunction<AttackManager> consumer,
    boolean printMessages) {
    if (soulFireServer.attacks().isEmpty()) {
      if (printMessages) {
        context.getSource().sendWarn("No attacks found!");
      }

      return 2;
    }

    var resultCode = Command.SINGLE_SUCCESS;
    for (var attackManager : soulFireServer.attacks().values()) {
      if (context.getSource().extraData.containsKey("attack_ids")
        && Arrays.stream(context.getSource().extraData.get("attack_ids").split(","))
        .mapToInt(Integer::parseInt)
        .noneMatch(i -> i == attackManager.id())) {
        continue;
      }

      if (printMessages) {
        context
          .getSource()
          .sendInfo("--- Running command for attack " + attackManager.id() + " ---");
      }

      var result = consumer.applyAsInt(attackManager);
      if (result != Command.SINGLE_SUCCESS) {
        resultCode = result;
      }
    }

    return resultCode;
  }

  public int forEveryAttackEnsureHasBots(
    CommandContext<ConsoleSubject> context, ToIntFunction<AttackManager> consumer) {
    return forEveryAttackEnsureHasBots(context, consumer, true);
  }

  private int forEveryAttackEnsureHasBots(
    CommandContext<ConsoleSubject> context,
    ToIntFunction<AttackManager> consumer,
    boolean printMessages) {
    return forEveryAttack(
      context,
      attackManager -> {
        if (attackManager.botConnections().isEmpty()) {
          if (printMessages) {
            context.getSource().sendWarn("No bots found in attack " + attackManager.id() + "!");
          }
          return 3;
        }

        return consumer.applyAsInt(attackManager);
      },
      printMessages);
  }

  public int forEveryBot(
    CommandContext<ConsoleSubject> context, ToIntFunction<BotConnection> consumer) {
    return forEveryBot(context, consumer, true);
  }

  private int forEveryBot(
    CommandContext<ConsoleSubject> context,
    ToIntFunction<BotConnection> consumer,
    boolean printMessages) {
    return forEveryAttackEnsureHasBots(
      context,
      attackManager -> {
        var resultCode = Command.SINGLE_SUCCESS;
        for (var bot : attackManager.botConnections().values()) {
          if (context.getSource().extraData.containsKey("bot_names")
            && Arrays.stream(context.getSource().extraData.get("bot_names").split(","))
            .noneMatch(s -> s.equals(bot.meta().accountName()))) {
            continue;
          }

          if (printMessages) {
            context
              .getSource()
              .sendInfo("--- Running command for bot " + bot.meta().accountName() + " ---");
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

  public int executePathfinding(CommandContext<ConsoleSubject> context, GoalScorer goalScorer) {
    return forEveryBot(
      context,
      bot -> {
        executePathfinding(bot, goalScorer);
        return Command.SINGLE_SUCCESS;
      });
  }

  public static void executePathfinding(BotConnection bot, GoalScorer goalScorer) {
    PathExecutor.executePathfinding(bot, goalScorer, new CompletableFuture<>());
  }

  @Override
  public List<Map.Entry<Instant, String>> getCommandHistory() {
    synchronized (commandHistory) {
      return List.copyOf(commandHistory);
    }
  }

  @Override
  public int execute(String command) {
    command = command.strip();

    try {
      var result = dispatcher.execute(command, new ConsoleSubject());
      commandHistory.add(Map.entry(Instant.now(), command));

      // Only save successful commands
      if (result == Command.SINGLE_SUCCESS) {
        newCommandHistoryEntry(command);
      }

      return result;
    } catch (CommandSyntaxException e) {
      log.warn(e.getMessage());
      return Command.SINGLE_SUCCESS;
    }
  }

  private void loadCommandHistory() {
    synchronized (commandHistory) {
      commandHistory.clear();
      try {
        if (!Files.exists(HISTORY_FILE)) {
          return;
        }

        var lines = Files.readAllLines(HISTORY_FILE);
        for (var line : lines) {
          var firstColon = line.indexOf(':');
          if (firstColon == -1) {
            continue;
          }

          var seconds = Long.parseLong(line.substring(0, firstColon));
          var command = line.substring(firstColon + 1);

          commandHistory.add(Map.entry(Instant.ofEpochSecond(seconds), command));
        }
      } catch (IOException e) {
        log.error("Failed to create command history file!", e);
      }
    }
  }

  private void newCommandHistoryEntry(String command) {
    synchronized (commandHistory) {
      try {
        Files.createDirectories(HISTORY_FILE.getParent());
        var newLine = Instant.now().getEpochSecond() + ":" + command + System.lineSeparator();
        Files.writeString(
          HISTORY_FILE, newLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
        log.error("Failed to create command history file!", e);
      }
    }
  }

  private void clearCommandHistory() {
    synchronized (commandHistory) {
      try {
        Files.deleteIfExists(HISTORY_FILE);
        commandHistory.clear();
      } catch (IOException e) {
        log.error("Failed to delete command history file!", e);
      }
    }
  }

  @Override
  public List<String> getCompletionSuggestions(String command) {
    return dispatcher
      .getCompletionSuggestions(dispatcher.parse(command, new ConsoleSubject()))
      .join()
      .getList()
      .stream()
      .map(Suggestion::getText)
      .toList();
  }

  private HelpData[] getAllUsage(
    final CommandNode<ConsoleSubject> node, final ConsoleSubject source) {
    final var result = new ArrayList<HelpData>();
    getAllUsage(node, source, result, "");
    return result.toArray(new HelpData[0]);
  }

  private void getAllUsage(
    final CommandNode<ConsoleSubject> node,
    final ConsoleSubject source,
    final ArrayList<HelpData> result,
    final String prefix) {
    if (!node.canUse(source)) {
      return;
    }

    if (node.getCommand() != null) {
      var helpWrapper = (CommandHelpWrapper) node.getCommand();
      if (!helpWrapper.privateCommand()) {
        result.add(new HelpData(prefix, helpWrapper.help()));
      }
    }

    if (node.getRedirect() != null) {
      var redirectHelpWrapper = (RedirectHelpWrapper) node.getRedirectModifier();
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
