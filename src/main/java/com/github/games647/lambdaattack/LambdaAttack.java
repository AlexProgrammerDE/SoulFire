package com.github.games647.lambdaattack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.spacehq.mc.auth.exception.request.RequestException;

public class LambdaAttack {

    public static final String PROJECT_NAME = "LambdaAttack";

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 25_565;
    private static final int DEFAULT_AMOUNT = 10;
    private static final int DEFAULT_DELAY = 0;

    private static final Logger logger = Logger.getLogger(PROJECT_NAME);
    private boolean running = true;
    private final List<Bot> clients = new ArrayList<>();

    public void start(String host, int port, int amount, int delay, String nameFormat) throws RequestException {
        for (int i = 0; i < amount; i++) {
            Bot bot = new Bot(String.format(nameFormat, i));
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

    public void stop() {
        this.running = false;

        clients.stream().forEach((client) -> client.close());
    }

    public static Logger getLogger() {
        return logger;
    }
}
