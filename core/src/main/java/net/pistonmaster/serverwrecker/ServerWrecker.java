package net.pistonmaster.serverwrecker;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.common.*;
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

        for (int i = 0; i < options.amount; i++) {
            Pair<String, String> userPassword;

            if (accounts == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
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
                    userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
                }
            }

            IPacketWrapper account = authenticate(options.gameVersion, userPassword.getLeft(), userPassword.getRight(), Proxy.NO_PROXY);
            if (account == null) {
                logger.warn("The account " + userPassword.getLeft() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }

            AbstractBot bot;
            if (!proxyCache.isEmpty()) {
                proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                BotProxy proxy = proxyIterator.next();

                if (options.accountsPerProxy > 0) {
                    proxyUseMap.putIfAbsent(proxy, new AtomicInteger());
                    while (proxyUseMap.get(proxy).get() >= options.accountsPerProxy) {
                        proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                        proxy = proxyIterator.next();
                        proxyUseMap.putIfAbsent(proxy, new AtomicInteger());

                        if (!proxyIterator.hasNext() && proxyUseMap.get(proxy).get() >= options.accountsPerProxy) {
                            break;
                        }
                    }

                    proxyUseMap.get(proxy).incrementAndGet();

                    if (proxyUseMap.size() == proxyCache.size() && isFull(proxyUseMap, options.accountsPerProxy)) {
                        logger.warn("All proxies in use now! Limiting amount size now...");
                        break;
                    }
                }

                bot = new BotFactory().createBot(options, account, proxy.getAddress(), logger, serviceServer, options.proxyType, proxy.getUsername(), proxy.getPassword());
            } else {
                bot = new BotFactory().createBot(options, account, logger, serviceServer);
            }

            if (bot == null) {
                continue;
            }

            this.clients.add(bot);
        }

        if (proxyCache.isEmpty()) {
            logger.info("Starting attack at {} with {} bots", options.hostname, clients.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", options.hostname, clients.size(), proxyUseMap.size());
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
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (!running) {
                break;
            }

            if (options.debug) {
                logger.debug("Connecting bot {}", i);
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
