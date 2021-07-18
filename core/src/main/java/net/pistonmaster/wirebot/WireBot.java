package net.pistonmaster.wirebot;

import com.github.steveice10.packetlib.ProxyInfo;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WireBot {

    public static final String PROJECT_NAME = "WireBot";

    private static final Logger LOGGER = Logger.getLogger(PROJECT_NAME);
    private static final WireBot instance = new WireBot();
    private final List<Bot> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean running = false;

    private List<ProxyInfo> proxies;
    private List<String> names;

    public static Logger getLogger() {
        return LOGGER;
    }

    public static WireBot getInstance() {
        return instance;
    }

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

            IPacketWrapper account = authenticate(options.gameVersion, username, "");

            Bot bot;
            if (proxies != null) {
                ProxyInfo proxy = proxies.get(i % proxies.size());
                bot = new Bot(options, account, proxy, LOGGER);
            } else {
                bot = new Bot(options, account, LOGGER);
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

    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password) {
        if (!password.isEmpty()) {
            throw new UnsupportedOperationException("Not implemented");
//            return new MinecraftProtocol(username, password);
//            LOGGER.info("Successfully authenticated user");
        } else {
            return UniversalFactory.authenticate(gameVersion, username);
        }
    }

    public void setProxies(List<ProxyInfo> proxies) {
        this.proxies = proxies;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public void stop() {
        this.running = false;
        clients.forEach(Bot::disconnect);
        clients.clear();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
