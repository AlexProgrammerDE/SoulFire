/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.common;

import org.slf4j.Logger;

public record SessionEventBus(Options options, Logger log,
                              AbstractBot bot) {
    public static final char COMMAND_IDENTIFIER = '/';

    public void onChat(String message) {
        log.info("Received Message: {}", message);
    }

    public void onPosition(double x, double y, double z, float pitch, float yaw) {
        bot.setLocation(new EntityLocation(x, y, z, pitch, yaw));
    }

    public void onHealth(float health, float food) {
        bot.setHealth(health);
        bot.setFood(food);
    }

    public void onJoin() {
        if (options.autoRegister()) {
            String password = "ServerWrecker"; // TODO
            bot.sendMessage(COMMAND_IDENTIFIER + "register " + password + ' ' + password);
            bot.sendMessage(COMMAND_IDENTIFIER + "login " + password);
        }
    }

    public void onDisconnect(String reason, Throwable cause) {
        if (cause.getClass().getSimpleName().equals("UnexpectedEncryptionException")) {
            log.info("Server is online mode!");
        } else {
            log.info("Disconnected: {}", reason);
        }

        if (options.debug()) {
            log.warn("Bot disconnected with cause: ", cause);
        }
    }
}
