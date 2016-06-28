package com.github.games647.lambdaattack;

import java.util.ArrayList;
import java.util.List;

import org.spacehq.mc.auth.exception.request.RequestException;

public class LambdaAttack {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 25_565;
    private static final int DEFAULT_AMOUNT = 10;
    private static final int DEFAULT_DELAY = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            new LambdaAttack().start(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_AMOUNT, DEFAULT_DELAY);
        } else if (args.length < 4) {
            System.out.println("Not enough arguments");
        } else {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            int amount = Integer.parseInt(args[2]);
            int delay = Integer.parseInt(args[3]) * 1000;

            new LambdaAttack().start(host, port, amount, delay);
        }
    }

    private final List<Bot> clients = new ArrayList<>();

    public void start(String host, int port, int amount, int delay) throws RequestException {
        for (int i = 1; i <= amount; i++) {
            try {
                //in seconds
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Bot bot = new Bot("Bot" + i);
            bot.connect(host, port);
            this.clients.add(bot);
        }
    }

    public void stop() {
        clients.stream().forEach((client) -> client.getSession().disconnect("Disconnect"));
    }
}
