/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableList;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.base.BaseVersionProvider;
import lombok.Getter;
import lombok.Setter;
import net.kyori.event.EventBus;
import net.kyori.event.SimpleEventBus;
import net.pistonmaster.serverwrecker.api.event.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.AttackStartEvent;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerEvent;
import net.pistonmaster.serverwrecker.common.*;
import net.pistonmaster.serverwrecker.logging.LogUtil;
import net.pistonmaster.serverwrecker.protocol.*;
import net.pistonmaster.serverwrecker.viaversion.SWViaPlatform;
import net.pistonmaster.serverwrecker.viaversion.StorableOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ServerWrecker {
    public static final String PROJECT_NAME = "ServerWrecker";
    public static final String VERSION = "0.0.2";
    @Getter
    private static final Logger logger = LoggerFactory.getLogger(PROJECT_NAME);
    @Getter
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final EventBus<ServerWreckerEvent> eventBus = new SimpleEventBus<>(ServerWreckerEvent.class);
    private final List<Bot> bots = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final List<BotProxy> passWordProxies = new ArrayList<>();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private boolean running = false;
    @Setter
    private boolean paused = false;
    @Setter
    private List<String> accounts;
    @Setter
    private ServiceServer serviceServer = ServiceServer.MOJANG;

    public ServerWrecker(Path dataFolder) {
        injector.register(ServerWrecker.class, this);
        setupLogging(Level.INFO);

        /*
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() != RequestorType.PROXY)
                    return null;

                Optional<InetSocketAddress> optional = passWordProxies.keySet().stream().filter(address -> address.getAddress().equals(getRequestingSite())).findFirst();
                return optional.map(passWordProxies::get).orElse(null);
            }
        });*/
        Path viaPath = dataFolder.resolve("viaversion");
        SWViaPlatform platform = new SWViaPlatform(viaPath);
        ViaPlatformLoader loader = new ViaPlatformLoader() {
            @Override
            public void load() {
                Via.getManager().getProviders().use(VersionProvider.class, new BaseVersionProvider() {
                    @Override
                    public int getClosestServerProtocol(UserConnection connection) {
                        StorableOptions options = connection.get(StorableOptions.class);
                        Objects.requireNonNull(options, "StorableOptions is null");
                        System.out.println("Protocol: " + options.options().protocolVersion().getVersion());

                        return options.options().protocolVersion().getVersion();
                    }
                });
            }

            @Override
            public void unload() {
            }
        };
        Via.init(ViaManagerImpl.builder()
                .platform(platform)
                .injector(platform.getInjector())
                        .loader(loader)
                .build());

        platform.init();

        ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();
        manager.onServerLoaded();
    }

    public void start(SWOptions options) {
        if (options.debug()) {
            setupLogging(Level.DEBUG);
        } else {
            setupLogging(Level.INFO);
        }

        this.running = true;

        List<BotProxy> proxyCache = passWordProxies.isEmpty() ? Collections.emptyList() : ImmutableList.copyOf(passWordProxies);
        Iterator<BotProxy> proxyIterator = proxyCache.listIterator();
        Map<BotProxy, AtomicInteger> proxyUseMap = new HashMap<>();

        for (int i = 1; i <= options.amount(); i++) {
            Pair<String, String> userPassword;

            if (accounts == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat(), i), "");
            } else {
                if (accounts.size() <= i) {
                    logger.warn("Amount is higher than the name list size. Limiting amount size now...");
                    break;
                }

                String[] lines = accounts.get(i).split(":");

                if (lines.length == 1) {
                    userPassword = new Pair<>(lines[0], "");
                } else if (lines.length == 2) {
                    userPassword = new Pair<>(lines[0], lines[1]);
                } else {
                    userPassword = new Pair<>(String.format(options.botNameFormat(), i), "");
                }
            }

            ProtocolWrapper account = authenticate(userPassword.left(), userPassword.right(), Proxy.NO_PROXY);
            if (account == null) {
                logger.warn("The account " + userPassword.left() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }

            Bot bot;
            if (!proxyCache.isEmpty()) {
                proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                BotProxy proxy = proxyIterator.next();

                if (options.accountsPerProxy() > 0) {
                    proxyUseMap.putIfAbsent(proxy, new AtomicInteger());
                    while (proxyUseMap.get(proxy).get() >= options.accountsPerProxy()) {
                        proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                        proxy = proxyIterator.next();
                        proxyUseMap.putIfAbsent(proxy, new AtomicInteger());

                        if (!proxyIterator.hasNext() && proxyUseMap.get(proxy).get() >= options.accountsPerProxy()) {
                            break;
                        }
                    }

                    proxyUseMap.get(proxy).incrementAndGet();

                    if (proxyUseMap.size() == proxyCache.size() && isFull(proxyUseMap, options.accountsPerProxy())) {
                        logger.warn("All proxies in use now! Limiting amount size now...");
                        break;
                    }
                }

                bot = new BotFactory().createBot(options, account, proxy.address(), serviceServer, options.proxyType(), proxy.username(), proxy.password());
            } else {
                bot = new BotFactory().createBot(options, account, serviceServer);
            }

            this.bots.add(bot);
        }

        if (proxyCache.isEmpty()) {
            logger.info("Starting attack at {} with {} bots", options.hostname(), bots.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", options.hostname(), bots.size(), proxyUseMap.size());
        }

        eventBus.post(new AttackStartEvent());

        for (Bot bot : bots) {
            try {
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            while (paused) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Stop the bot in case the user aborted the attack
            if (!running) {
                break;
            }

            bot.getLogger().info("Connecting...");

            bot.connect(options.hostname(), options.port(), new SessionEventBus(options, bot.getLogger(), bot));
        }
    }

    public ProtocolWrapper authenticate(String username, String password, Proxy proxy) {
        if (password.isEmpty()) {
            return AuthFactory.authenticate(username);
        } else {
            try {
                return AuthFactory.authenticate(username, password, proxy, serviceServer, serviceServerConfig);
            } catch (Exception e) {
                logger.warn("Failed to authenticate " + username + "! (" + e.getMessage() + ")", e);
                return null;
            }
        }
    }

    public void stop() {
        this.running = false;
        bots.forEach(Bot::disconnect);
        bots.clear();
        eventBus.post(new AttackEndEvent());
    }

    private boolean isFull(Map<BotProxy, AtomicInteger> map, int limit) {
        for (Map.Entry<BotProxy, AtomicInteger> entry : map.entrySet()) {
            if (entry.getValue().get() < limit) {
                return false;
            }
        }

        return true;
    }

    private Iterator<BotProxy> fromStartIfNoNext(Iterator<BotProxy> iterator, List<BotProxy> proxyList) {
        return iterator.hasNext() ? iterator : proxyList.listIterator();
    }

    public void setupLogging(Level level) {
        LogUtil.setLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, level);
        LogUtil.setLevel(logger, level);
        LogUtil.setLevel("io.netty", level);
        LogUtil.setLevel("org.pf4j", level);
    }
}
