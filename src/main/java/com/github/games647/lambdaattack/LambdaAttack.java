package com.github.games647.lambdaattack;

import java.util.ArrayList;
import java.util.List;

import org.spacehq.mc.auth.exception.request.RequestException;

public class LambdaAttack {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 25565;
    private static final int DEFAULT_AMOUNT = 3;
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
            int delay = Integer.parseInt(args[3]);

            new LambdaAttack().start(host, port, amount, delay);
        }
    }

    private final List<Bot> clients = new ArrayList<>();

    public void start(String host, int port, int amount, int delay) throws InterruptedException, RequestException {
        for (int i = 1; i <= amount; i++) {
            //in seconds
            Thread.sleep(delay * 1_000);

            Bot bot = new Bot("Bot " + i);
            bot.connect(host, port);
            this.clients.add(bot);
        }

        Thread.sleep(3 * 1_000);
        clients.stream().forEach((client) -> client.getSession().disconnect("Disconnect"));
    }
}
