package net.pistonmaster.wirebot;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.packetlib.ProxyInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.Options;
import net.pistonmaster.wirebot.common.Pair;

import javax.swing.*;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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

    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private JFrame window;

    public static Logger getLogger() {
        return LOGGER;
    }

    public static WireBot getInstance() {
        return instance;
    }

    public void start(Options options) {
        running = true;

        for (int i = 0; i < options.amount; i++) {
            Pair<String, String> userPassword;

            if (names == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
            } else {
                if (names.size() <= i) {
                    LOGGER.warning("Amount is higher than the name list size. Limiting amount size now...");
                    break;
                }

                String lines[] = names.get(i).split(":");

                if (lines.length == 1) {
                    userPassword = new Pair<>(lines[0], "");
                } else if (lines.length == 2) {
                    userPassword = new Pair<>(lines[0], lines[1]);
                } else {
                    userPassword = new Pair<>(String.format(options.botNameFormat, i), "");
                }
            }

            IPacketWrapper account = authenticate(options.gameVersion, userPassword.getLeft(), userPassword.getRight(), Proxy.NO_PROXY);

            Bot bot;
            if (proxies != null) {
                ProxyInfo proxy = proxies.get(i % proxies.size());
                bot = new Bot(options, account, proxy, LOGGER);
            } else {
                bot = new Bot(options, account, LOGGER);
            }

            SessionService sessionService = new SessionService();
            sessionService.setBaseUri(ServiceServer.MOJANG.getSession());
            bot.getSession().setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);

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

    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy) {
        if (password.isEmpty()) {
            return UniversalFactory.authenticate(gameVersion, username);
        } else {
            try {
                return UniversalFactory.authenticate(gameVersion, username, password, proxy, ServiceServer.MOJANG);
            } catch (RequestException e) {
                LOGGER.log(Level.WARNING, "Failed to authenticate " + username + "!", e);
                return null;
            }
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
