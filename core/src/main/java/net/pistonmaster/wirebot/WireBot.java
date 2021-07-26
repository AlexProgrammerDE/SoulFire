package net.pistonmaster.wirebot;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.wirebot.common.*;
import net.pistonmaster.wirebot.protocol.BotFactory;
import net.pistonmaster.wirebot.protocol_legacy.BotLegacy;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WireBot {
    public static final String PROJECT_NAME = "WireBot";

    private static final Logger LOGGER = Logger.getLogger(PROJECT_NAME);
    private static final WireBot instance = new WireBot();
    private final List<AbstractBot> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    @Getter
    private boolean running = false;
    @Getter
    @Setter
    private boolean paused = false;

    private List<InetSocketAddress> proxies;
    private List<String> accounts;

    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private JFrame window;

    @Getter
    @Setter
    private ServiceServer serviceServer = ServiceServer.MOJANG;

    public static Logger getLogger() {
        return LOGGER;
    }

    public static WireBot getInstance() {
        return instance;
    }

    public void start(Options options) {
        running = true;

        List<InetSocketAddress> proxyCache = proxies == null ? null : new ArrayList<>(proxies);

        Map<InetSocketAddress, AtomicInteger> proxyUseMap = new HashMap<>();

        for (int i = 0; i < options.amount; i++) {
            Pair<String, String> userPassword;

            if (accounts == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
            } else {
                if (accounts.size() <= i) {
                    LOGGER.warning("Amount is higher than the name list size. Limiting amount size now...");
                    break;
                }

                String[] lines = accounts.get(i).split(":");

                if (lines.length == 1) {
                    userPassword = new Pair<>(lines[0], "");
                } else if (lines.length == 2) {
                    userPassword = new Pair<>(lines[0], lines[1]);
                } else {
                    userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
                }
            }

            IPacketWrapper account = authenticate(options.gameVersion, userPassword.getLeft(), userPassword.getRight(), Proxy.NO_PROXY);

            if (account == null) {
                LOGGER.warning("The account " + userPassword.getLeft() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }

            AbstractBot bot = null;

            switch (options.gameVersion) {
                case VERSION_1_8, VERSION_1_9, VERSION_1_10:
                    bot = new BotLegacy(options, account, LOGGER);
                    break;
                case VERSION_1_11, VERSION_1_12, VERSION_1_13, VERSION_1_14, VERSION_1_15, VERSION_1_16, VERSION_1_17:
                    if (proxies != null) {
                        InetSocketAddress proxy;

                        if (options.accountsPreProxy <= 0) {
                            proxy = proxyCache.get(i % proxyCache.size());
                        } else {
                            if (proxyUseMap.size() == proxies.size() && isFull(proxyUseMap, options.accountsPreProxy)) {
                                LOGGER.warning("All proxies in use now! Limiting amount size now...");
                                break;
                            }

                            proxy = proxyCache.get(i % proxyCache.size());

                            proxyUseMap.putIfAbsent(proxy, new AtomicInteger());

                            AtomicInteger proxyUse = proxyUseMap.get(proxy);

                            while (proxyUse.get() >= options.accountsPreProxy) {
                                proxy = proxyCache.get(i % proxyCache.size());

                                proxyUseMap.putIfAbsent(proxy, new AtomicInteger());

                                proxyUse = proxyUseMap.get(proxy);
                            }

                            proxyUseMap.get(proxy).incrementAndGet();
                        }

                        bot = new BotFactory().createBot(options, account, proxy, LOGGER, serviceServer, options.proxyType);
                    } else {
                        bot = new BotFactory().createBot(options, account, LOGGER, serviceServer);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + options.gameVersion);
            }

            if (bot == null) {
                continue;
            }

            this.clients.add(bot);
        }

        for (AbstractBot client : clients) {
            while (paused) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (!running) {
                break;
            }

            client.connect(options.hostname, options.port);
        }
    }

    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy) {
        if (password.isEmpty()) {
            return UniversalFactory.authenticate(gameVersion, username);
        } else {
            try {
                return UniversalFactory.authenticate(gameVersion, username, password, proxy, serviceServer);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to authenticate " + username + "! (" + e.getMessage() + ")", e);
                return null;
            }
        }
    }

    public void setProxies(List<InetSocketAddress> proxies) {
        this.proxies = proxies;
    }

    public void setAccounts(List<String> accounts) {
        this.accounts = accounts;
    }

    public void stop() {
        this.running = false;
        clients.forEach(AbstractBot::disconnect);
        clients.clear();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    private boolean isFull(Map<InetSocketAddress, AtomicInteger> map, int limit) {
        for (Map.Entry<InetSocketAddress, AtomicInteger> entry : map.entrySet()) {
            if (entry.getValue().get() < limit) {
                return false;
            }
        }

        return true;
    }
}
