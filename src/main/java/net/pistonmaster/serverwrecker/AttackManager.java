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
package net.pistonmaster.serverwrecker;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.viaversion.viaversion.api.Via;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.state.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.state.AttackStartEvent;
import net.pistonmaster.serverwrecker.auth.AccountList;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.common.AttackState;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.BotConnectionFactory;
import net.pistonmaster.serverwrecker.protocol.netty.ResolveUtil;
import net.pistonmaster.serverwrecker.protocol.netty.SWNettyHelper;
import net.pistonmaster.serverwrecker.proxy.ProxyList;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackManager {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private static final GameProfile EMPTY_GAME_PROFILE = new GameProfile((UUID) null, "DoNotUseGameProfile");
    private final Logger logger = LoggerFactory.getLogger("AttackManager-" + ID_COUNTER.getAndIncrement());
    private final List<BotConnection> botConnections = new CopyOnWriteArrayList<>();
    private final ServerWrecker serverWrecker;
    @Setter
    private AttackState attackState = AttackState.STOPPED;

    @SuppressWarnings("UnstableApiUsage")
    public void start(SettingsHolder settingsHolder) {
        if (!attackState.isStopped()) {
            throw new IllegalStateException("Attack is already running");
        }

        AccountList accountListSettings = settingsHolder.get(AccountList.class);
        List<MinecraftAccount> accounts = new ArrayList<>(accountListSettings.accounts()
                .stream().filter(MinecraftAccount::enabled).toList());

        ProxyList proxyListSettings = settingsHolder.get(ProxyList.class);
        List<SWProxy> proxies = new ArrayList<>(proxyListSettings.proxies()
                .stream().filter(SWProxy::enabled).toList());

        BotSettings botSettings = settingsHolder.get(BotSettings.class);
        DevSettings devSettings = settingsHolder.get(DevSettings.class);
        AccountSettings accountSettings = settingsHolder.get(AccountSettings.class);
        ProxySettings proxySettings = settingsHolder.get(ProxySettings.class);

        Via.getManager().debugHandler().setEnabled(devSettings.debug());
        serverWrecker.setupLogging(devSettings.debug() ? Level.DEBUG : Level.INFO);

        this.attackState = AttackState.RUNNING;

        logger.info("Preparing bot attack at {}", botSettings.host());

        int botAmount = botSettings.amount(); // How many bots to connect
        int botsPerProxy = proxySettings.botsPerProxy(); // How many bots per proxy are allowed
        int availableProxiesCount = proxies.size(); // How many proxies are available?
        int maxBots = botsPerProxy > 0 ? botsPerProxy * availableProxiesCount : botAmount; // How many bots can be used at max

        if (botAmount > maxBots) {
            logger.warn("You have specified {} bots, but only {} are available.", botAmount, maxBots);
            logger.warn("You need {} more proxies to run this amount of bots.", (botAmount - maxBots) / botsPerProxy);
            logger.warn("Continuing with {} bots.", maxBots);
            botAmount = maxBots;
        }

        int availableAccounts = accounts.size();

        if (availableAccounts > 0 && botAmount > availableAccounts) {
            logger.warn("You have specified {} bots, but only {} accounts are available.", botAmount, availableAccounts);
            logger.warn("Continuing with {} bots.", availableAccounts);
            botAmount = availableAccounts;
        }

        if (accountSettings.shuffleAccounts()) {
            Collections.shuffle(accounts);
        }

        Map<SWProxy, Integer> proxyUseMap = new Object2IntOpenHashMap<>();
        for (SWProxy proxy : proxies) {
            proxyUseMap.put(proxy, 0);
        }

        // Prepare an event loop group with enough threads for the attack
        int threads = botAmount;
        threads *= 2; // We need a monitor thread for each bot

        EventLoopGroup attackEventLoopGroup = SWNettyHelper.createEventLoopGroup(threads, "Attack-Thread");

        boolean isBedrock = SWConstants.isBedrock(botSettings.protocolVersion());
        InetSocketAddress targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsHolder, attackEventLoopGroup);

        Queue<BotConnectionFactory> factories = new ArrayBlockingQueue<>(botAmount);
        for (int botId = 1; botId <= botAmount; botId++) {
            SWProxy proxyData = getProxy(botsPerProxy, proxyUseMap);
            MinecraftAccount minecraftAccount = getAccount(accountSettings, accounts, botId);

            // AuthData will be used internally instead of the MCProtocol data
            MinecraftProtocol protocol = new MinecraftProtocol(EMPTY_GAME_PROFILE, null);

            // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
            protocol.setUseDefaultListeners(false);

            factories.add(new BotConnectionFactory(
                    this,
                    targetAddress,
                    settingsHolder,
                    LoggerFactory.getLogger(minecraftAccount.username()),
                    protocol,
                    minecraftAccount,
                    proxyData,
                    attackEventLoopGroup
            ));
        }

        if (availableProxiesCount == 0) {
            logger.info("Starting attack at {} with {} bots", botSettings.host(), factories.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", botSettings.host(), factories.size(), availableProxiesCount);
        }

        ServerWreckerAPI.postEvent(new AttackStartEvent());

        // Used for concurrent bot connecting
        ExecutorService connectService = Executors.newFixedThreadPool(botSettings.concurrentConnects());

        boolean isFirst = true;
        Random random = ThreadLocalRandom.current();
        while (!factories.isEmpty()) {
            BotConnectionFactory factory = factories.poll();
            if (factory == null) {
                break;
            }

            if (!isFirst) {
                try {
                    TimeUnit.MILLISECONDS.sleep(random.nextInt(botSettings.minJoinDelayMs(), botSettings.maxJoinDelayMs()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.debug("Scheduling bot {}", factory.minecraftAccount().username());
            connectService.execute(() -> {
                if (attackState.isStopped()) {
                    return;
                }

                TimeUtil.waitCondition(attackState::isPaused);

                logger.debug("Connecting bot {}", factory.minecraftAccount().username());
                try {
                    botConnections.add(factory.connect().join());
                } catch (Throwable e) {
                    logger.error("Error while connecting", e);
                }
            });

            isFirst = false;
        }
    }

    private MinecraftAccount getAccount(AccountSettings accountSettings, List<MinecraftAccount> accounts, int botId) {
        if (accounts.isEmpty()) {
            return new MinecraftAccount(String.format(accountSettings.nameFormat(), botId));
        }

        return accounts.remove(0);
    }

    private SWProxy getProxy(int accountsPerProxy, Map<SWProxy, Integer> proxyUseMap) {
        if (proxyUseMap.isEmpty()) {
            return null; // No proxies available
        } else {
            SWProxy selectedProxy = proxyUseMap.entrySet().stream()
                    .filter(entry -> accountsPerProxy == -1 || entry.getValue() < accountsPerProxy)
                    .min(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElseThrow(() -> new IllegalStateException("No proxies available!")); // Should never happen

            // Always present
            proxyUseMap.computeIfPresent(selectedProxy, (proxy, useCount) -> useCount + 1);

            return selectedProxy;
        }
    }

    public CompletableFuture<Void> stop() {
        if (attackState.isStopped()) {
            return CompletableFuture.completedFuture(null);
        }

        serverWrecker.getLogger().info("Stopping bot attack");
        this.attackState = AttackState.STOPPED;

        return CompletableFuture.runAsync(this::stopInternal);
    }

    private void stopInternal() {
        logger.info("Disconnecting bots");
        for (BotConnection botConnection : botConnections) {
            botConnection.disconnect();
        }

        logger.info("Shutting down task executors");
        for (BotConnection botConnection : botConnections) {
            botConnection.executorManager().shutdownAll();
        }

        logger.info("Shutting down attack event loop group");
        var shutdownFutures = new ArrayList<Future<?>>();
        for (BotConnection botConnection : botConnections) {
            Future<?> future = botConnection.session().getEventLoopGroup().shutdownGracefully();
            shutdownFutures.add(future);
        }

        logger.info("Waiting for attack event loops to fully shut down");
        for (Future<?> future : shutdownFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error while shutting down", e);
            }
        }

        // Leave them for GC to clean up
        botConnections.clear();

        // Notify addons of state change
        ServerWreckerAPI.postEvent(new AttackEndEvent());

        logger.info("Attack stopped");
    }
}
