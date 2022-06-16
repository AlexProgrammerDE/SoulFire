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

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.common.*;
import net.pistonmaster.serverwrecker.protocol.AuthFactory;
import net.pistonmaster.serverwrecker.protocol.BotFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.Proxy;
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
    private static final ServerWrecker instance = new ServerWrecker();
    private final List<AbstractBot> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final List<BotProxy> passWordProxies = new ArrayList<>();
    private boolean running = false;
    @Setter
    private boolean paused = false;
    @Setter
    private List<String> accounts;
    @Setter(value = AccessLevel.PROTECTED)
    private JFrame window;
    @Setter
    private ServiceServer serviceServer = ServiceServer.MOJANG;
    private final Map<String, String> serviceServerConfig = new HashMap<>();

    public ServerWrecker() {
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.netty")).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.pf4j")).setLevel(Level.INFO);

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
    }

    public void start(Options options) {
        running = true;

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

            IPacketWrapper account = authenticate(options.gameVersion(), userPassword.left(), userPassword.right(), Proxy.NO_PROXY);
            if (account == null) {
                logger.warn("The account " + userPassword.left() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }

            AbstractBot bot;
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

            this.clients.add(bot);
        }

        if (proxyCache.isEmpty()) {
            logger.info("Starting attack at {} with {} bots", options.hostname(), clients.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", options.hostname(), clients.size(), proxyUseMap.size());
        }

        int i = 0;
        for (AbstractBot client : clients) {
            i++;

            while (paused) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Stop the bot in case the user aborted the attack
            if (!running) {
                break;
            }

            logger.info("Connecting bot {}", i);

            client.connect(options.hostname(), options.port());
        }
    }

    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy) {
        if (password.isEmpty()) {
            return AuthFactory.authenticate(gameVersion, username);
        } else {
            try {
                return AuthFactory.authenticate(gameVersion, username, password, proxy, serviceServer, serviceServerConfig);
            } catch (Exception e) {
                logger.warn("Failed to authenticate " + username + "! (" + e.getMessage() + ")", e);
                return null;
            }
        }
    }

    public void stop() {
        this.running = false;
        clients.forEach(AbstractBot::disconnect);
        clients.clear();
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
}
