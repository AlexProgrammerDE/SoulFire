package com.github.games647.lambdaattack.common;

import lombok.RequiredArgsConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SessionEventBus {
    public static final char COMMAND_IDENTIFIER = '/';

    private final Options options;
    private final Logger log;
    private final IBot bot;

    public void onChat(String message) {
        log.log(Level.INFO, "Received Message: {0}", message);
    }

    public void onPosition(double x, double y, double z, float pitch, float yaw) {
        bot.setLocation(new EntitiyLocation(x, y, z, pitch, yaw));
    }

    public void onHealth(float health, float food) {
        bot.setHealth(health);
        bot.setFood(health);
    }

    public void onJoin() {
        if (options.autoRegister) {
            String password = "LambdaAttack"; // TODO
            bot.sendMessage(COMMAND_IDENTIFIER + "register " + password + ' ' + password);
            bot.sendMessage(COMMAND_IDENTIFIER + "login " + password);
        }
    }

    public void onDisconnect(String reason) {
        log.log(Level.INFO, "Disconnected: {0}", reason);
    }
}
