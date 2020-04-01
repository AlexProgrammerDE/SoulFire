package com.github.games647.lambdaattack;

import com.github.games647.lambdaattack.bot.Bot;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LambdaAttack {

    public static final String PROJECT_NAME = "LambdaAttack";

    private static final Logger LOGGER = Logger.getLogger(PROJECT_NAME);
    private static final LambdaAttack instance = new LambdaAttack();

    public static Logger getLogger() {
        return LOGGER;
    }

    public static LambdaAttack getInstance() {
        return instance;
    }

    private boolean running = false;

    private List<Proxy> proxies;
    private List<String> names;

    private final List<Bot> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start(Options options) {
        running = true;

        for (int i = 0; i < options.amount; i++) {
            String username = String.format(options.botNameFormat, i);
            if (names != null) {
                if (names.size() <= i) {
                    LOGGER.warning("Amount is higher than the name list size. Limitting amount size now...");
                    break;
                }

                username = names.get(i);
            }

            UniversalProtocol account = authenticate(options.gameVersion, username, "");

            Bot bot;
            if (proxies != null) {
                Proxy proxy = proxies.get(i % proxies.size());
                bot = new Bot(options, account, proxy);
            } else {
                bot = new Bot(options, account);
            }

            this.clients.add(bot);
        }

        for (Bot client : clients) {
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

    public UniversalProtocol authenticate(GameVersion gameVersion, String username, String password) {
        UniversalProtocol protocol;
        if (!password.isEmpty()) {
            throw new UnsupportedOperationException("Not implemented");
//            protocol = new MinecraftProtocol(username, password);
//            LOGGER.info("Successfully authenticated user");
        } else {
            protocol = UniversalFactory.authenticate(gameVersion, username);
        }

        return protocol;
    }

    public void setProxies(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public void stop() {
        this.running = false;
        clients.stream().forEach(Bot::disconnect);
        clients.clear();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
