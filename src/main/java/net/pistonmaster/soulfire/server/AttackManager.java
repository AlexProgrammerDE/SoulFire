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

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.account.service.SWOfflineAuthService;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.api.AttackState;
import net.pistonmaster.soulfire.server.api.event.EventExceptionHandler;
import net.pistonmaster.soulfire.server.api.event.SoulFireAttackEvent;
import net.pistonmaster.soulfire.server.api.event.attack.AttackEndedEvent;
import net.pistonmaster.soulfire.server.api.event.attack.AttackStartEvent;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.protocol.BotConnectionFactory;
import net.pistonmaster.soulfire.server.protocol.netty.ResolveUtil;
import net.pistonmaster.soulfire.server.protocol.netty.SWNettyHelper;
import net.pistonmaster.soulfire.server.settings.AccountSettings;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.settings.ProxySettings;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.util.RandomUtil;
import net.pistonmaster.soulfire.server.util.TimeUtil;
import net.pistonmaster.soulfire.server.viaversion.SWVersionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackManager {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private final int id = ID_COUNTER.getAndIncrement();
    private final Logger logger = LoggerFactory.getLogger("AttackManager-" + id);
    private final LambdaManager eventBus = LambdaManager.basic(new ASMGenerator())
            .setExceptionHandler(EventExceptionHandler.INSTANCE)
            .setEventFilter((c, h) -> {
                if (SoulFireAttackEvent.class.isAssignableFrom(c)) {
                    return true;
                } else {
                    throw new IllegalStateException("This event handler only accepts attack events");
                }
            });
    private final List<BotConnection> botConnections = new CopyOnWriteArrayList<>();
    private final SoulFireServer soulFireServer;
    @Setter
    private AttackState attackState = AttackState.STOPPED;

    private static MinecraftAccount getAccount(SettingsHolder settingsHolder, List<MinecraftAccount> accounts, int botId) {
        if (accounts.isEmpty()) {
            return SWOfflineAuthService.createAccount(String.format(settingsHolder.get(AccountSettings.NAME_FORMAT), botId));
        }

        return accounts.removeFirst();
    }

    private static Optional<SWProxy> getProxy(int accountsPerProxy, Object2IntMap<SWProxy> proxyUseMap) {
        if (proxyUseMap.isEmpty()) {
            return Optional.empty(); // No proxies available
        }

        var selectedProxy = proxyUseMap.object2IntEntrySet().stream()
                .filter(entry -> accountsPerProxy == -1 || entry.getIntValue() < accountsPerProxy)
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow(() -> new IllegalStateException("No proxies available!")); // Should never happen

        // Always present
        selectedProxy.setValue(selectedProxy.getIntValue() + 1);

        return Optional.of(selectedProxy.getKey());
    }

    public CompletableFuture<?> start(SettingsHolder settingsHolder) {
        if (!attackState.isStopped()) {
            throw new IllegalStateException("Attack is already running");
        }

        var accounts = settingsHolder.accounts().stream()
                .filter(MinecraftAccount::enabled)
                .collect(Collectors.toCollection(ArrayList::new));
        var proxies = settingsHolder.proxies().stream()
                .filter(SWProxy::enabled)
                .collect(Collectors.toCollection(ArrayList::new));

        SoulFireServer.setupLoggingAndVia(settingsHolder);

        this.attackState = AttackState.RUNNING;

        var address = settingsHolder.get(BotSettings.ADDRESS);
        logger.info("Preparing bot attack at {}", address);

        var botAmount = settingsHolder.get(BotSettings.AMOUNT); // How many bots to connect
        var botsPerProxy = settingsHolder.get(ProxySettings.BOTS_PER_PROXY); // How many bots per proxy are allowed
        var availableProxiesCount = proxies.size(); // How many proxies are available?
        var maxBots = botsPerProxy > 0 ? botsPerProxy * availableProxiesCount : botAmount; // How many bots can be used at max

        if (botAmount > maxBots) {
            logger.warn("You have specified {} bots, but only {} are available.", botAmount, maxBots);
            logger.warn("You need {} more proxies to run this amount of bots.", (botAmount - maxBots) / botsPerProxy);
            logger.warn("Continuing with {} bots.", maxBots);
            botAmount = maxBots;
        }

        var availableAccounts = accounts.size();

        if (availableAccounts > 0 && botAmount > availableAccounts) {
            logger.warn("You have specified {} bots, but only {} accounts are available.", botAmount, availableAccounts);
            logger.warn("Continuing with {} bots.", availableAccounts);
            botAmount = availableAccounts;
        }

        if (settingsHolder.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
            Collections.shuffle(accounts);
        }

        var proxyUseMap = new Object2IntOpenHashMap<SWProxy>();
        for (var proxy : proxies) {
            proxyUseMap.put(proxy, 0);
        }

        // Prepare an event loop group with enough threads for the attack
        var threads = botAmount;
        threads *= 2; // We need a monitor thread for each bot

        var attackEventLoopGroup = SWNettyHelper.createEventLoopGroup(threads, String.format("Attack-%d", id));

        var protocolVersion = settingsHolder.get(BotSettings.PROTOCOL_VERSION, ProtocolVersion::getClosest);
        var isBedrock = SWVersionConstants.isBedrock(protocolVersion);
        var targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsHolder, attackEventLoopGroup);

        var factories = new ArrayBlockingQueue<BotConnectionFactory>(botAmount);
        for (var botId = 1; botId <= botAmount; botId++) {
            var proxyData = getProxy(botsPerProxy, proxyUseMap).orElse(null);
            var minecraftAccount = getAccount(settingsHolder, accounts, botId);

            // AuthData will be used internally instead of the MCProtocol data
            var protocol = new MinecraftProtocol();

            // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
            protocol.setUseDefaultListeners(false);

            factories.add(new BotConnectionFactory(
                    this,
                    targetAddress.orElseThrow(() -> new IllegalStateException("Could not resolve address")),
                    settingsHolder,
                    LoggerFactory.getLogger(minecraftAccount.username()),
                    protocol,
                    minecraftAccount,
                    proxyData,
                    attackEventLoopGroup
            ));
        }

        if (availableProxiesCount == 0) {
            logger.info("Starting attack at {} with {} bots", address, factories.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", address, factories.size(), availableProxiesCount);
        }

        eventBus.call(new AttackStartEvent(this));

        // Used for concurrent bot connecting
        var connectService = Executors.newFixedThreadPool(settingsHolder.get(BotSettings.CONCURRENT_CONNECTS));

        return CompletableFuture.runAsync(() -> {
            while (!factories.isEmpty()) {
                var factory = factories.poll();
                if (factory == null) {
                    break;
                }

                logger.debug("Scheduling bot {}", factory.minecraftAccount().username());
                connectService.execute(() -> {
                    if (attackState.isStopped()) {
                        return;
                    }

                    TimeUtil.waitCondition(attackState::isPaused);

                    logger.debug("Connecting bot {}", factory.minecraftAccount().username());
                    var botConnection = factory.prepareConnection();
                    botConnections.add(botConnection);

                    try {
                        botConnection.connect().get();
                    } catch (Throwable e) {
                        logger.error("Error while connecting", e);
                    }
                });

                TimeUtil.waitTime(RandomUtil.getRandomInt(settingsHolder.get(BotSettings.JOIN_DELAY.min()), settingsHolder.get(BotSettings.JOIN_DELAY.max())), TimeUnit.MILLISECONDS);
            }
        });
    }

    public CompletableFuture<?> stop() {
        if (attackState.isStopped()) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Stopping bot attack");
        this.attackState = AttackState.STOPPED;

        return CompletableFuture.runAsync(this::stopInternal);
    }

    private void stopInternal() {
        logger.info("Disconnecting bots");
        do {
            var eventLoopGroups = new HashSet<EventLoopGroup>();
            var disconnectFuture = new ArrayList<CompletableFuture<?>>();
            for (var botConnection : List.copyOf(botConnections)) {
                disconnectFuture.add(botConnection.gracefulDisconnect());
                eventLoopGroups.add(botConnection.session().eventLoopGroup());
                botConnections.remove(botConnection);
            }

            logger.info("Waiting for all bots to fully disconnect");
            for (var future : disconnectFuture) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error while shutting down", e);
                }
            }

            logger.info("Shutting down attack event loop groups");
            for (var eventLoopGroup : eventLoopGroups) {
                try {
                    eventLoopGroup.shutdownGracefully().get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error while shutting down", e);
                }
            }
        } while (!botConnections.isEmpty()); // To make sure really all bots are disconnected

        // Notify plugins of state change
        eventBus.call(new AttackEndedEvent(this));

        logger.info("Attack stopped");
    }
}
