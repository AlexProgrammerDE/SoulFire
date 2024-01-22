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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.brigadier.CommandHelpWrapper;
import net.pistonmaster.soulfire.brigadier.ConsoleSubject;
import net.pistonmaster.soulfire.client.gui.LogPanel;
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
import net.pistonmaster.soulfire.util.SWPathConstants;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.vector.Vector3d;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.function.ToIntFunction;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.*;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager {
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final SoulFireServer soulFireServer;
    private final List<Map.Entry<Instant, String>> commandHistory = Collections.synchronizedList(new ArrayList<>());
    private final Path targetFile = SWPathConstants.DATA_FOLDER.resolve(".command_history");

    @PostConstruct
    public void postConstruct() {
        loadCommandHistory();

        // Help
        dispatcher.register(literal("help")
                .executes(help("Prints a list of all available commands", c -> {
                    c.getSource().sendMessage("Available commands:");
                    for (var command : getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(String.format("%s: %s", command.command(), command.help()));
                    }

                    return Command.SINGLE_SUCCESS;
                })));
        dispatcher.register(literal("help-markdown")
                .executes(privateCommand(c -> {
                    for (var command : getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(String.format("| `%s` | %s |", command.command(), command.help()));
                    }

                    return Command.SINGLE_SUCCESS;
                })));

        // GUI and CLI commands
        dispatcher.register(literal("reload-history").executes(help("Refreshes the loaded command history from the data file", c -> {
            loadCommandHistory();

            return Command.SINGLE_SUCCESS;
        })));
        dispatcher.register(literal("clear-history").executes(help("Wipes the command history data", c -> {
            clearCommandHistory();

            return Command.SINGLE_SUCCESS;
        })));
        dispatcher.register(literal("clear-console").executes(help("Clears the GUIs log panel", c -> {
            var logPanel = soulFireServer.injector().getIfAvailable(LogPanel.class);
            if (logPanel != null) {
                logPanel.messageLogPanel().clear();
            }

            return Command.SINGLE_SUCCESS;
        })));

        // Pathfinding
        dispatcher.register(literal("walkxyz")
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(help("Makes all connected bots walk to the xyz coordinates", c -> {
                                            var x = IntegerArgumentType.getInteger(c, "x");
                                            var y = IntegerArgumentType.getInteger(c, "y");
                                            var z = IntegerArgumentType.getInteger(c, "z");

                                            return executePathfinding(new PosGoal(x, y, z));
                                        }))))));
        dispatcher.register(literal("walkxz")
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer())
                                .executes(help("Makes all connected bots walk to the xz coordinates", c -> {
                                    var x = IntegerArgumentType.getInteger(c, "x");
                                    var z = IntegerArgumentType.getInteger(c, "z");

                                    return executePathfinding(new XZGoal(x, z));
                                })))));
        dispatcher.register(literal("walky")
                .then(argument("y", IntegerArgumentType.integer())
                        .executes(help("Makes all connected bots walk to the y coordinates", c -> {
                            var y = IntegerArgumentType.getInteger(c, "y");
                            return executePathfinding(new YGoal(y));
                        }))));
        dispatcher.register(literal("stop-path")
                .executes(help("Makes all connected bots stop pathfinding", c -> forEveryBot(bot -> {
                    EventUtil.runAndAssertChanged(bot.eventBus(), () ->
                            bot.eventBus().unregisterAll(BotPreTickEvent.class, (clazz, o) -> {
                                if (PathExecutor.class.isAssignableFrom(clazz)) {
                                    ((PathExecutor) o.orElseThrow()).cancel();
                                    return true;
                                }

                                return false;
                            }));

                    bot.sessionDataManager().controlState().resetAll();

                    c.getSource().sendMessage("Stopped pathfinding for " + bot.meta().minecraftAccount().username());
                    return Command.SINGLE_SUCCESS;
                }))));

        // Movement controls
        dispatcher.register(literal("lookat")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(help("Makes all connected bots look at the block at the xyz coordinates", c -> {
                                            var x = DoubleArgumentType.getDouble(c, "x");
                                            var y = DoubleArgumentType.getDouble(c, "y");
                                            var z = DoubleArgumentType.getDouble(c, "z");

                                            return forEveryBot(bot -> {
                                                var sessionDataManager = bot.sessionDataManager();
                                                var clientEntity = sessionDataManager.clientEntity();

                                                clientEntity.lookAt(RotationOrigin.EYES, Vector3d.from(x, y, z));
                                                return Command.SINGLE_SUCCESS;
                                            });
                                        }))))));
        dispatcher.register(literal("forward")
                .executes(help("Makes all connected bots start walking forward", c -> forEveryBot(bot -> {
                    bot.sessionDataManager().controlState().forward(true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(literal("backward")
                .executes(help("Makes all connected bots start walking backwards", c -> forEveryBot(bot -> {
                    bot.sessionDataManager().controlState().backward(true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(literal("left")
                .executes(help("Makes all connected bots start walking left", c -> forEveryBot(bot -> {
                    bot.sessionDataManager().controlState().left(true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(literal("right")
                .executes(help("Makes all connected bots start walking right", c -> forEveryBot(bot -> {
                    bot.sessionDataManager().controlState().right(true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(literal("jump")
                .executes(help("Makes all connected bots jump up repeatedly", c -> forEveryBot(bot -> {
                    var sessionDataManager = bot.sessionDataManager();

                    sessionDataManager.controlState().jumping(true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(literal("reset")
                .executes(help("Resets the movement of all connected bots", c -> forEveryBot(bot -> {
                    bot.sessionDataManager().controlState().resetAll();
                    return Command.SINGLE_SUCCESS;
                }))));

        // Attack controls
        dispatcher.register(literal("stop-attack")
                .executes(help("Stops the ongoing attacks", c -> forEveryAttack(attackManager -> {
                    soulFireServer.stopAttack(attackManager.id());
                    return Command.SINGLE_SUCCESS;
                }))));

        // Utility commands
        dispatcher.register(literal("online").executes(help("Shows connected bots from all attacks", c -> forEveryAttackEnsureHasBots(attackManager -> {
            var online = new ArrayList<String>();
            for (var bot : attackManager.botConnections()) {
                if (!bot.isOnline()) {
                    continue;
                }

                online.add(bot.meta().minecraftAccount().username());
            }

            c.getSource().sendMessage(online.size() + " bots online: " + String.join(", ", online));
            return Command.SINGLE_SUCCESS;
        }))));
        dispatcher.register(literal("say")
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(help("Makes all connected bots send a message in chat or execute a command", c -> {
                            var message = StringArgumentType.getString(c, "message");

                            return forEveryBot(bot -> {
                                if (!bot.isOnline()) {
                                    return Command.SINGLE_SUCCESS;
                                }

                                bot.botControl().sendMessage(message);

                                return Command.SINGLE_SUCCESS;
                            });
                        }))));
        dispatcher.register(literal("stats").executes(help("Shows network stats", c -> forEveryAttackEnsureHasBots(attackManager -> {
            log.info("Total bots: {}", attackManager.botConnections().size());
            long readTraffic = 0;
            long writeTraffic = 0;
            for (var bot : attackManager.botConnections()) {
                var trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                readTraffic += trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                writeTraffic += trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
            }

            log.info("Total read traffic: {}", FileUtils.byteCountToDisplaySize(readTraffic));
            log.info("Total write traffic: {}", FileUtils.byteCountToDisplaySize(writeTraffic));

            long currentReadTraffic = 0;
            long currentWriteTraffic = 0;
            for (var bot : attackManager.botConnections()) {
                var trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                currentReadTraffic += trafficShapingHandler.trafficCounter().lastReadThroughput();
                currentWriteTraffic += trafficShapingHandler.trafficCounter().lastWriteThroughput();
            }

            log.info("Current read traffic: {}/s", FileUtils.byteCountToDisplaySize(currentReadTraffic));
            log.info("Current write traffic: {}/s", FileUtils.byteCountToDisplaySize(currentWriteTraffic));

            return Command.SINGLE_SUCCESS;
        }))));

        SoulFireAPI.postEvent(new DispatcherInitEvent(dispatcher));
    }

    private int forEveryAttack(ToIntFunction<AttackManager> consumer) {
        if (soulFireServer.attacks().isEmpty()) {
            log.warn("No attacks found!");
            return 2;
        }

        var resultCode = Command.SINGLE_SUCCESS;
        for (var attackManager : soulFireServer.attacks().values()) {
            log.info("--- Running command for attack {} ---", attackManager.id());
            var result = consumer.applyAsInt(attackManager);
            if (result != Command.SINGLE_SUCCESS) {
                resultCode = result;
            }
        }

        return resultCode;
    }

    private int forEveryAttackEnsureHasBots(ToIntFunction<AttackManager> consumer) {
        return forEveryAttack(attackManager -> {
            if (attackManager.botConnections().isEmpty()) {
                log.warn("No bots connected!");
                return 3;
            }

            return consumer.applyAsInt(attackManager);
        });
    }

    private int forEveryBot(ToIntFunction<BotConnection> consumer) {
        return forEveryAttackEnsureHasBots(attackManager -> {
            var resultCode = Command.SINGLE_SUCCESS;
            for (var bot : attackManager.botConnections()) {
                log.info("--- Running command for bot {} ---", bot.meta().minecraftAccount().username());
                var result = consumer.applyAsInt(bot);
                if (result != Command.SINGLE_SUCCESS) {
                    resultCode = result;
                }
            }

            return resultCode;
        });
    }

    private int executePathfinding(GoalScorer goalScorer) {
        return forEveryBot(bot -> {
            var logger = bot.logger();
            var executorService = bot.executorManager().newExecutorService(bot, "PathfindingManager");
            executorService.execute(() -> {
                var sessionDataManager = bot.sessionDataManager();
                var clientEntity = sessionDataManager.clientEntity();
                var routeFinder = new RouteFinder(
                        new MinecraftGraph(sessionDataManager.tagsState()),
                        goalScorer
                );

                Boolean2ObjectFunction<List<WorldAction>> findPath = requiresRepositioning -> {
                    var start = BotEntityState.initialState(
                            clientEntity,
                            new ProjectedLevelState(
                                    Objects.requireNonNull(sessionDataManager.getCurrentLevel(), "Level is null!").chunks().immutableCopy()
                            ),
                            new ProjectedInventory(
                                    sessionDataManager.inventoryManager().getPlayerInventory()
                            )
                    );
                    logger.info("Starting calculations at: {}", start);
                    var actions = routeFinder.findRoute(start, requiresRepositioning);
                    logger.info("Calculated path with {} actions: {}", actions.size(), actions);
                    return actions;
                };

                var pathExecutor = new PathExecutor(bot, findPath.get(true), findPath, executorService);
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
            var result = dispatcher.execute(command, ConsoleSubject.INSTANCE);
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
                Files.writeString(targetFile, newLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, ConsoleSubject.INSTANCE))
                .join()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }

    private HelpData[] getAllUsage(final CommandNode<ConsoleSubject> node, final ConsoleSubject source, final boolean restricted) {
        final var result = new ArrayList<HelpData>();
        getAllUsage(node, source, result, "", restricted);
        return result.toArray(new HelpData[0]);
    }

    private void getAllUsage(final CommandNode<ConsoleSubject> node, final ConsoleSubject source, final ArrayList<HelpData> result, final String prefix, final boolean restricted) {
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
            final var redirect = node.getRedirect() == dispatcher.getRoot() ? "..." : "-> " + node.getRedirect().getUsageText();
            result.add(new HelpData(prefix.isEmpty() ? node.getUsageText() + ARGUMENT_SEPARATOR + redirect : prefix + ARGUMENT_SEPARATOR + redirect, "N/A"));
        } else if (!node.getChildren().isEmpty()) {
            for (final var child : node.getChildren()) {
                getAllUsage(child, source, result, prefix.isEmpty() ? child.getUsageText() : prefix + ARGUMENT_SEPARATOR + child.getUsageText(), restricted);
            }
        }
    }

    private record HelpData(String command, String help) {
    }
}
