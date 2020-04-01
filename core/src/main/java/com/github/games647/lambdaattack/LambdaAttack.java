package com.github.games647.lambdaattack;

import com.github.games647.lambdaattack.bot.Bot;
import com.github.games647.lambdaattack.gui.MainGui;
import com.github.steveice10.mc.auth.exception.request.RequestException;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LambdaAttack {

    public static final String PROJECT_NAME = "LambdaAttack";

    private static LambdaAttack instance;
    private static final Logger LOGGER = Logger.getLogger(PROJECT_NAME);

    public static Logger getLogger() {
        return LOGGER;
    }

    public static LambdaAttack getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, null, throwable);
        });

        instance = new LambdaAttack();
        new MainGui(instance);
    }

    private boolean running = false;
    private GameVersion gameVersion = GameVersion.VERSION_1_15;

    private List<Proxy> proxies;
    private List<String> names;

    private boolean autoRegister = false;

    private final List<Bot> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start(String host, int port, int amount, int delay, String nameFormat) throws RequestException {
        running = true;

        for (int i = 0; i < amount; i++) {
            String username = String.format(nameFormat, i);
            if (names != null) {
                if (names.size() <= i) {
                    LOGGER.warning("Amount is higher than the name list size. Limitting amount size now...");
                    break;
                }

                username = names.get(i);
            }

            UniversalProtocol account = authenticate(username, "");

            Bot bot;
            if (proxies != null) {
                Proxy proxy = proxies.get(i % proxies.size());
                bot = new Bot(account, proxy);
            } else {
                bot = new Bot(account);
            }

            this.clients.add(bot);
        }

        for (Bot client : clients) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (!running) {
                break;
            }

            client.connect(host, port);
        }
    }

    public UniversalProtocol authenticate(String username, String password) throws RequestException {
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

    public GameVersion getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(GameVersion gameVersion) {
        this.gameVersion = gameVersion;
    }

    public void stop() {
        this.running = false;
        clients.stream().forEach(Bot::disconnect);
        clients.clear();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }
}
