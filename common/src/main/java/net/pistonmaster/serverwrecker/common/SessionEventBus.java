package net.pistonmaster.serverwrecker.common;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class SessionEventBus {
    public static final char COMMAND_IDENTIFIER = '/';

    private final Options options;
    private final Logger log;
    private final AbstractBot bot;

    public void onChat(String message) {
        log.info("Received Message: {}", message);
    }

    public void onPosition(double x, double y, double z, float pitch, float yaw) {
        bot.setLocation(new EntitiyLocation(x, y, z, pitch, yaw));
    }

    public void onHealth(float health, float food) {
        bot.setHealth(health);
        bot.setFood(food);
    }

    public void onJoin() {
        if (options.autoRegister) {
            String password = "ServerWrecker"; // TODO
            bot.sendMessage(COMMAND_IDENTIFIER + "register " + password + ' ' + password);
            bot.sendMessage(COMMAND_IDENTIFIER + "login " + password);
        }
    }

    public void onDisconnect(String reason, Throwable cause) {
        log.info("Disconnected: {}", reason);
        if (options.debug) {
            log.warn("Bot disconnected with cause: ", cause);
        }
    }
}
