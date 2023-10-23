/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.command;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ConsoleSubject;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.lifecycle.DispatcherInitEvent;
import net.pistonmaster.serverwrecker.gui.LogPanel;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.RouteFinder;
import net.pistonmaster.serverwrecker.pathfinding.execution.PathExecutor;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.pathfinding.goals.GoalScorer;
import net.pistonmaster.serverwrecker.pathfinding.goals.PosGoal;
import net.pistonmaster.serverwrecker.pathfinding.goals.XZGoal;
import net.pistonmaster.serverwrecker.pathfinding.goals.YGoal;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.serverwrecker.pathfinding.graph.ProjectedLevelState;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.vector.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR;
import static net.pistonmaster.serverwrecker.command.BrigadierHelper.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final ServerWrecker serverWrecker;
    private final ConsoleSubject consoleSubject;
    private final List<String> commandHistory = Collections.synchronizedList(new ArrayList<>());
    private final Path targetFile = ServerWrecker.DATA_FOLDER.resolve(".command_history");

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
                    return 1;
                })));
        dispatcher.register(literal("help-markdown")
                .executes(privateCommand(c -> {
                    for (var command : getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(String.format("| `%s` | %s |", command.command(), command.help()));
                    }
                    return 1;
                })));

        // GUI and CLI commands
        dispatcher.register(literal("reload-history").executes(help("Refreshes the loaded command history from the data file", c -> {
            loadCommandHistory();
            return 1;
        })));
        dispatcher.register(literal("clear-history").executes(help("Wipes the command history data", c -> {
            clearCommandHistory();
            return 1;
        })));
        dispatcher.register(literal("clear-console").executes(help("Clears the GUIs log panel", c -> {
            var logPanel = serverWrecker.getInjector().getIfAvailable(LogPanel.class);
            if (logPanel != null) {
                logPanel.getMessageLogPanel().clear();
            }
            return 1;
        })));

        // Pathfinding
        dispatcher.register(literal("walkxyz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(help("Makes all connected bots pathfind to the xyz coordinates", c -> {
                                            var x = DoubleArgumentType.getDouble(c, "x");
                                            var y = DoubleArgumentType.getDouble(c, "y");
                                            var z = DoubleArgumentType.getDouble(c, "z");

                                            return executePathfinding(new PosGoal(x, y, z));
                                        }))))));
        dispatcher.register(literal("walkxz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                                .executes(help("Makes all connected bots pathfind to the xz coordinates", c -> {
                                    var x = DoubleArgumentType.getDouble(c, "x");
                                    var z = DoubleArgumentType.getDouble(c, "z");

                                    return executePathfinding(new XZGoal(x, z));
                                })))));
        dispatcher.register(literal("walky")
                .then(argument("y", DoubleArgumentType.doubleArg())
                        .executes(help("Makes all connected bots pathfind to the y coordinates", c -> {
                            var y = DoubleArgumentType.getDouble(c, "y");
                            return executePathfinding(new YGoal(y));
                        }))));
        dispatcher.register(literal("stop-path")
                .executes(help("Makes all connected bots stop pathfinding", c -> {
                    var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        LOGGER.warn("No attack found!");
                        return 2;
                    }

                    if (attackManager.getBotConnections().isEmpty()) {
                        LOGGER.info("No bots connected!");
                        return 3;
                    }

                    for (var bot : attackManager.getBotConnections()) {
                        bot.eventBus().unsubscribeIf(p -> {
                            if (PathExecutor.class.isInstance(p)) {
                                PathExecutor.class.cast(p).cancel();

                                return true;
                            } else {
                                return false;
                            }
                        });
                        bot.sessionDataManager().getBotMovementManager().getControlState().resetAll();
                    }
                    return 1;
                })));

        // Movement controls
        dispatcher.register(literal("lookat")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(help("Makes all connected bots look at the block at the xyz coordinates", c -> {
                                            var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                                            if (attackManager == null) {
                                                LOGGER.warn("No attack found!");
                                                return 2;
                                            }

                                            if (attackManager.getBotConnections().isEmpty()) {
                                                LOGGER.info("No bots connected!");
                                                return 3;
                                            }

                                            var x = DoubleArgumentType.getDouble(c, "x");
                                            var y = DoubleArgumentType.getDouble(c, "y");
                                            var z = DoubleArgumentType.getDouble(c, "z");

                                            for (var bot : attackManager.getBotConnections()) {
                                                var sessionDataManager = bot.sessionDataManager();
                                                var botMovementManager = sessionDataManager.getBotMovementManager();

                                                botMovementManager.lookAt(RotationOrigin.FEET, Vector3d.from(x, y, z));
                                            }
                                            return 1;
                                        }))))));
        dispatcher.register(literal("forward")
                .executes(help("Makes all connected bots start walking forward", c -> {
                    var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        LOGGER.warn("No attack found!");
                        return 2;
                    }

                    if (attackManager.getBotConnections().isEmpty()) {
                        LOGGER.info("No bots connected!");
                        return 3;
                    }

                    for (var bot : attackManager.getBotConnections()) {
                        var sessionDataManager = bot.sessionDataManager();
                        var botMovementManager = sessionDataManager.getBotMovementManager();

                        botMovementManager.getControlState().setForward(true);
                    }
                    return 1;
                })));
        dispatcher.register(literal("jump")
                .executes(help("Makes all connected bots jump up repeatedly", c -> {
                    var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        LOGGER.warn("No attack found!");
                        return 2;
                    }

                    if (attackManager.getBotConnections().isEmpty()) {
                        LOGGER.info("No bots connected!");
                        return 3;
                    }

                    for (var bot : attackManager.getBotConnections()) {
                        var sessionDataManager = bot.sessionDataManager();
                        var botMovementManager = sessionDataManager.getBotMovementManager();

                        botMovementManager.getControlState().setJumping(true);
                    }
                    return 1;
                })));
        dispatcher.register(literal("reset")
                .executes(help("Resets the movement of all connected bots", c -> {
                    var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        LOGGER.warn("No attack found!");
                        return 2;
                    }

                    if (attackManager.getBotConnections().isEmpty()) {
                        LOGGER.info("No bots connected!");
                        return 3;
                    }

                    for (var bot : attackManager.getBotConnections()) {
                        var sessionDataManager = bot.sessionDataManager();
                        var botMovementManager = sessionDataManager.getBotMovementManager();

                        botMovementManager.getControlState().resetAll();
                    }
                    return 1;
                })));

        // Attack controls
        dispatcher.register(literal("start-attack")
                .executes(help("Starts an attack using the current profile", c -> {
                    var id = serverWrecker.startAttack();
                    LOGGER.info("Started an attack with id {}", id);
                    return 1;
                })));
        dispatcher.register(literal("stop-attack")
                .executes(help("Stops the ongoing attack", c -> {
                    var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        LOGGER.warn("No attack found!");
                        return 2;
                    }

                    serverWrecker.stopAttack(attackManager.getId());
                    return 1;
                })));

        // Utility commands
        dispatcher.register(literal("online").executes(help("Shows connected bots from all attacks", c -> {
            var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

            if (attackManager == null) {
                LOGGER.warn("No attack found!");
                return 2;
            }

            var online = new ArrayList<String>();
            attackManager.getBotConnections().forEach(client -> {
                if (client.isOnline()) {
                    online.add(client.meta().getMinecraftAccount().username());
                }
            });
            c.getSource().sendMessage(online.size() + " bots online: " + String.join(", ", online));
            return 1;
        })));
        dispatcher.register(literal("say")
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(help("Makes all connected bots send a message in chat or execute a command", c -> {
                            var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                            if (attackManager == null) {
                                LOGGER.warn("No attack found!");
                                return 2;
                            }

                            if (attackManager.getBotConnections().isEmpty()) {
                                LOGGER.info("No bots connected!");
                                return 3;
                            }

                            var message = StringArgumentType.getString(c, "message");
                            LOGGER.info("Sending message by all bots: '{}'", message);

                            attackManager.getBotConnections().forEach(client -> {
                                if (client.isOnline()) {
                                    client.botControl().sendMessage(message);
                                }
                            });
                            return 1;
                        }))));
        dispatcher.register(literal("stats").executes(help("Shows network stats", c -> {
            var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

            if (attackManager == null) {
                LOGGER.warn("No attack found!");
                return 2;
            }

            if (attackManager.getBotConnections().isEmpty()) {
                LOGGER.info("No bots connected!");
                return 3;
            }

            LOGGER.info("Total bots: {}", attackManager.getBotConnections().size());
            long readTraffic = 0;
            long writeTraffic = 0;
            for (var bot : attackManager.getBotConnections()) {
                var trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                readTraffic += trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                writeTraffic += trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
            }

            LOGGER.info("Total read traffic: {}", FileUtils.byteCountToDisplaySize(readTraffic));
            LOGGER.info("Total write traffic: {}", FileUtils.byteCountToDisplaySize(writeTraffic));

            long currentReadTraffic = 0;
            long currentWriteTraffic = 0;
            for (var bot : attackManager.getBotConnections()) {
                var trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                currentReadTraffic += trafficShapingHandler.trafficCounter().lastReadThroughput();
                currentWriteTraffic += trafficShapingHandler.trafficCounter().lastWriteThroughput();
            }

            LOGGER.info("Current read traffic: {}/s", FileUtils.byteCountToDisplaySize(currentReadTraffic));
            LOGGER.info("Current write traffic: {}/s", FileUtils.byteCountToDisplaySize(currentWriteTraffic));

            return 1;
        })));

        ServerWreckerAPI.postEvent(new DispatcherInitEvent(dispatcher));
    }

    private int executePathfinding(GoalScorer goalScorer) {
        var attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

        if (attackManager == null) {
            LOGGER.warn("No attack found!");
            return 2;
        }

        if (attackManager.getBotConnections().isEmpty()) {
            LOGGER.info("No bots connected!");
            return 3;
        }

        for (var bot : attackManager.getBotConnections()) {
            var logger = bot.logger();
            var executorService = bot.executorManager().newExecutorService("Pathfinding");
            executorService.execute(() -> {
                var sessionDataManager = bot.sessionDataManager();
                var botMovementManager = sessionDataManager.getBotMovementManager();
                var routeFinder = new RouteFinder(new MinecraftGraph(sessionDataManager.getTagsState()), goalScorer);

                Boolean2ObjectFunction<List<WorldAction>> findPath = requiresRepositioning -> {
                    var start = BotEntityState.initialState(
                            botMovementManager.getPlayerPos(),
                            new ProjectedLevelState(
                                    Objects.requireNonNull(sessionDataManager.getCurrentLevel(), "Level is null!")
                            ),
                            new ProjectedInventory(
                                    sessionDataManager.getInventoryManager().getPlayerInventory()
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
        }

        return 1;
    }

    public List<String> getCommandHistory() {
        synchronized (commandHistory) {
            return List.copyOf(commandHistory);
        }
    }

    public int execute(String command) {
        command = command.strip();

        try {
            var result = dispatcher.execute(command, consoleSubject);
            commandHistory.add(command);

            // Only save successful commands
            if (result == 1) {
                newCommandHistoryEntry(command);
            }

            return result;
        } catch (CommandSyntaxException e) {
            LOGGER.warn(e.getMessage());
            return 1;
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

                    commandHistory.add(line.substring(firstColon + 1));
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create command history file!", e);
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
                LOGGER.error("Failed to create command history file!", e);
            }
        }
    }

    private void clearCommandHistory() {
        synchronized (commandHistory) {
            try {
                Files.deleteIfExists(targetFile);
                commandHistory.clear();
            } catch (IOException e) {
                LOGGER.error("Failed to delete command history file!", e);
            }
        }
    }

    public List<String> getCompletionSuggestions(String command) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, consoleSubject)).join().getList()
                .stream().map(Suggestion::getText).toList();
    }

    public HelpData[] getAllUsage(final CommandNode<ConsoleSubject> node, final ConsoleSubject source, final boolean restricted) {
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
