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
package net.pistonmaster.serverwrecker.logging;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
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
import java.util.function.Supplier;

import static net.pistonmaster.serverwrecker.logging.BrigadierHelper.argument;
import static net.pistonmaster.serverwrecker.logging.BrigadierHelper.literal;

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
        dispatcher.register(literal("reload-history").executes(c -> {
            loadCommandHistory();
            return 1;
        }));
        dispatcher.register(literal("clear-history").executes(c -> {
            clearCommandHistory();
            return 1;
        }));
        dispatcher.register(literal("online").executes(c -> {
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
        }));
        dispatcher.register(literal("clear-console").executes(c -> {
            var logPanel = serverWrecker.getInjector().getIfAvailable(LogPanel.class);
            if (logPanel != null) {
                logPanel.getMessageLogPanel().clear();
            }
            return 1;
        }));
        dispatcher.register(literal("say")
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(c -> {
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
                        })));
        dispatcher.register(literal("stats").executes(c -> {
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
        }));
        dispatcher.register(literal("help")
                .executes(c -> {
                    c.getSource().sendMessage("Available commands:");
                    for (var command : dispatcher.getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(command);
                    }
                    return 1;
                }));
        dispatcher.register(literal("walkxyz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> {
                                            var x = DoubleArgumentType.getDouble(c, "x");
                                            var y = DoubleArgumentType.getDouble(c, "y");
                                            var z = DoubleArgumentType.getDouble(c, "z");

                                            return executePathfinding(new PosGoal(x, y, z));
                                        })))));
        dispatcher.register(literal("walkxz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                                .executes(c -> {
                                    var x = DoubleArgumentType.getDouble(c, "x");
                                    var z = DoubleArgumentType.getDouble(c, "z");

                                    return executePathfinding(new XZGoal(x, z));
                                }))));
        dispatcher.register(literal("walky")
                .then(argument("y", DoubleArgumentType.doubleArg())
                        .executes(c -> {
                            var y = DoubleArgumentType.getDouble(c, "y");
                            return executePathfinding(new YGoal(y));
                        })));
        dispatcher.register(literal("lookat")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> {
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
                                        })))));
        dispatcher.register(literal("forward")
                .executes(c -> {
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
                }));
        dispatcher.register(literal("jump")
                .executes(c -> {
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
                }));
        dispatcher.register(literal("reset")
                .executes(c -> {
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
                }));
        dispatcher.register(literal("stop-path")
                .executes(c -> {
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
                }));
        dispatcher.register(literal("stop-attack")
                .executes(c -> {
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
                }));

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

                Supplier<List<WorldAction>> findPath = () -> {
                    var start = BotEntityState.initialState(
                            botMovementManager.getPlayerPos(),
                            new ProjectedLevelState(
                                    Objects.requireNonNull(sessionDataManager.getCurrentLevel(), "Level is null!")
                            ),
                            new ProjectedInventory(
                                    sessionDataManager.getInventoryManager().getPlayerInventory()
                            )
                    );
                    logger.info("Start: {}", start);
                    var actions = routeFinder.findRoute(start);
                    logger.info("Calculated path with {} actions: {}", actions.size(), actions);
                    return actions;
                };

                var pathExecutor = new PathExecutor(bot, findPath.get(), findPath, executorService);
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
            } catch (IOException e) {
                LOGGER.error("Failed to delete command history file!", e);
            }
        }
    }

    public List<String> getCompletionSuggestions(String command) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, consoleSubject)).join().getList()
                .stream().map(Suggestion::getText).toList();
    }
}
