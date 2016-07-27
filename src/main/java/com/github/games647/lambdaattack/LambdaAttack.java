package com.github.games647.lambdaattack;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;

public class LambdaAttack {

    public static final String PROJECT_NAME = "LambdaAttack";

    private static final Logger LOGGER = Logger.getLogger(PROJECT_NAME);

    public static Logger getLogger() {
        return LOGGER;
    }
    
    private boolean running = true;
    private final List<Bot> clients = new ArrayList<>();
    private List<Proxy> proxies;

    public void start(String host, int port, int amount, int delay, String nameFormat) throws RequestException {
        for (int i = 0; i < amount; i++) {
            MinecraftProtocol account = authenticate(String.format(nameFormat, i), "");
            
            Bot bot;
            if (proxies != null) {
                Proxy proxy = proxies.get((i + proxies.size()) % 4);
                bot = new Bot(account, proxy);
            } else {
                bot = new Bot(account);
            }

            this.clients.add(bot);
        }

        for (Bot client : clients) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (!running) {
                break;
            }

            client.connect(host, port);
        }
    }

    public MinecraftProtocol authenticate(String username, String password) throws RequestException {
        MinecraftProtocol protocol;
        if (!password.isEmpty()) {
            protocol = new MinecraftProtocol(username, password);
            LOGGER.info("Successfully authenticated user");
        } else {
            protocol = new MinecraftProtocol(username);
        }

        return protocol;
    }

    public void setProxies(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    public void stop() {
        this.running = false;

        clients.stream().forEach((client) -> client.close());
    }
}
