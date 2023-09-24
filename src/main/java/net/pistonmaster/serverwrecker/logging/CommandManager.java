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
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.AttackManager;
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
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import org.apache.commons.io.FileUtils;
import org.cloudburstmc.math.vector.Vector3d;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static net.pistonmaster.serverwrecker.logging.BrigadierHelper.argument;
import static net.pistonmaster.serverwrecker.logging.BrigadierHelper.literal;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandManager {
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final ServerWrecker serverWrecker;
    private final ConsoleSubject consoleSubject;
    private final List<String> commandHistory = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void postConstruct() {
        Logger logger = serverWrecker.getLogger();
        dispatcher.register(literal("online").executes(c -> {
            AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

            if (attackManager == null) {
                return 1;
            }

            List<String> online = new ArrayList<>();
            attackManager.getBotConnections().forEach(client -> {
                if (client.isOnline()) {
                    online.add(client.meta().getMinecraftAccount().username());
                }
            });
            c.getSource().sendMessage(online.size() + " bots online: " + String.join(", ", online));
            return 1;
        }));
        dispatcher.register(literal("clear").executes(c -> {
            LogPanel logPanel = serverWrecker.getInjector().getIfAvailable(LogPanel.class);
            if (logPanel != null) {
                logPanel.getMessageLogPanel().clear();
            }
            return 1;
        }));
        dispatcher.register(literal("say")
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(c -> {
                            AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                            if (attackManager == null) {
                                return 1;
                            }

                            String message = StringArgumentType.getString(c, "message");
                            logger.info("Sending message by all bots: '{}'", message);

                            attackManager.getBotConnections().forEach(client -> {
                                if (client.isOnline()) {
                                    client.botControl().sendMessage(message);
                                }
                            });
                            return 1;
                        })));
        dispatcher.register(literal("stats").executes(c -> {
            AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

            if (attackManager == null) {
                return 1;
            }

            if (attackManager.getBotConnections().isEmpty()) {
                logger.info("No bots connected!");
                return 1;
            }

            logger.info("Total bots: {}", attackManager.getBotConnections().size());
            long readTraffic = 0;
            long writeTraffic = 0;
            for (BotConnection bot : attackManager.getBotConnections()) {
                GlobalTrafficShapingHandler trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                readTraffic += trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                writeTraffic += trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
            }

            logger.info("Total read traffic: {}", FileUtils.byteCountToDisplaySize(readTraffic));
            logger.info("Total write traffic: {}", FileUtils.byteCountToDisplaySize(writeTraffic));

            long currentReadTraffic = 0;
            long currentWriteTraffic = 0;
            for (BotConnection bot : attackManager.getBotConnections()) {
                GlobalTrafficShapingHandler trafficShapingHandler = bot.getTrafficHandler();

                if (trafficShapingHandler == null) {
                    continue;
                }

                currentReadTraffic += trafficShapingHandler.trafficCounter().lastReadThroughput();
                currentWriteTraffic += trafficShapingHandler.trafficCounter().lastWriteThroughput();
            }

            logger.info("Current read traffic: {}/s", FileUtils.byteCountToDisplaySize(currentReadTraffic));
            logger.info("Current write traffic: {}/s", FileUtils.byteCountToDisplaySize(currentWriteTraffic));

            return 1;
        }));
        dispatcher.register(literal("help")
                .executes(c -> {
                    c.getSource().sendMessage("Available commands:");
                    for (String command : dispatcher.getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(command);
                    }
                    return 1;
                }));
        dispatcher.register(literal("walkxyz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> {
                                            double x = DoubleArgumentType.getDouble(c, "x");
                                            double y = DoubleArgumentType.getDouble(c, "y");
                                            double z = DoubleArgumentType.getDouble(c, "z");

                                            executePathfinding(new PosGoal(x, y, z));
                                            return 1;
                                        })))));
        dispatcher.register(literal("walkxz")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                                .executes(c -> {
                                    double x = DoubleArgumentType.getDouble(c, "x");
                                    double z = DoubleArgumentType.getDouble(c, "z");

                                    executePathfinding(new XZGoal(x, z));
                                    return 1;
                                }))));
        dispatcher.register(literal("walky")
                .then(argument("y", DoubleArgumentType.doubleArg())
                        .executes(c -> {
                            double y = DoubleArgumentType.getDouble(c, "y");
                            executePathfinding(new YGoal(y));
                            return 1;
                        })));
        dispatcher.register(literal("lookat")
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> {
                                            AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                                            if (attackManager == null) {
                                                return 1;
                                            }

                                            double x = DoubleArgumentType.getDouble(c, "x");
                                            double y = DoubleArgumentType.getDouble(c, "y");
                                            double z = DoubleArgumentType.getDouble(c, "z");

                                            for (BotConnection bot : attackManager.getBotConnections()) {
                                                SessionDataManager sessionDataManager = bot.sessionDataManager();
                                                BotMovementManager botMovementManager = sessionDataManager.getBotMovementManager();

                                                botMovementManager.lookAt(RotationOrigin.FEET, Vector3d.from(x, y, z));
                                            }
                                            return 1;
                                        })))));
        dispatcher.register(literal("forward")
                .executes(c -> {
                    AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        return 1;
                    }

                    for (BotConnection bot : attackManager.getBotConnections()) {
                        SessionDataManager sessionDataManager = bot.sessionDataManager();
                        BotMovementManager botMovementManager = sessionDataManager.getBotMovementManager();

                        botMovementManager.getControlState().setForward(true);
                    }
                    return 1;
                }));
        dispatcher.register(literal("stop")
                .executes(c -> {
                    AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

                    if (attackManager == null) {
                        return 1;
                    }

                    for (BotConnection bot : attackManager.getBotConnections()) {
                        SessionDataManager sessionDataManager = bot.sessionDataManager();
                        BotMovementManager botMovementManager = sessionDataManager.getBotMovementManager();

                        botMovementManager.getControlState().resetAll();
                    }
                    return 1;
                }));

        ServerWreckerAPI.postEvent(new DispatcherInitEvent(dispatcher));
    }

    private void executePathfinding(GoalScorer goalScorer) {
        AttackManager attackManager = serverWrecker.getAttacks().values().stream().findFirst().orElse(null);

        if (attackManager == null) {
            return;
        }

        for (BotConnection bot : attackManager.getBotConnections()) {
            Logger logger = bot.logger();
            bot.executorManager().newExecutorService("Pathfinding").execute(() -> {
                SessionDataManager sessionDataManager = bot.sessionDataManager();
                BotMovementManager botMovementManager = sessionDataManager.getBotMovementManager();
                RouteFinder routeFinder = new RouteFinder(new MinecraftGraph(), goalScorer);

                Supplier<List<WorldAction>> findPath = () -> {
                    BotEntityState start = new BotEntityState(botMovementManager.getPlayerPos(), new ProjectedLevelState(
                            sessionDataManager.getCurrentLevel()
                    ), new ProjectedInventory(
                            sessionDataManager.getInventoryManager().getPlayerInventory()
                    ));
                    logger.info("Start: {}", start);
                    List<WorldAction> actions = routeFinder.findRoute(start);
                    logger.info("Calculated path with {} actions: {}", actions.size(), actions);
                    return actions;
                };

                PathExecutor pathExecutor = new PathExecutor(bot, findPath.get(), findPath);
                pathExecutor.register();
            });
        }
    }

    public List<String> getCommandHistory() {
        synchronized (commandHistory) {
            return List.copyOf(commandHistory);
        }
    }

    public int execute(String command) {
        try {
            commandHistory.add(command);
            return dispatcher.execute(command, consoleSubject);
        } catch (CommandSyntaxException e) {
            serverWrecker.getLogger().warn(e.getMessage());
            return 1;
        }
    }

    public List<String> getCompletionSuggestions(String command) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, consoleSubject)).join().getList()
                .stream().map(Suggestion::getText).toList();
    }
}
