/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
    public void onChat(String message) {
        try {
            log.info("Received Message: {}", message);
        } catch (Exception e) {
            log.error("Error while logging message", e);
        }
    }

    public void onPosition(double x, double y, double z, float pitch, float yaw) {
        try {
            bot.setLocation(new EntityLocation(x, y, z, pitch, yaw));
        } catch (Exception e) {
            log.error("Error while logging position", e);
        }
    }

    public void onHealth(float health, float food) {
        try {
            bot.setHealth(health);
            bot.setFood(food);
        } catch (Exception e) {
            log.error("Error while logging health", e);
        }
    }

    public void onJoin() {
        try {
            if (options.autoRegister()) {
                String password = options.passwordFormat();

                // TODO: Listen for messages
                // TODO: Add more password options
                bot.sendMessage(options.registerCommand().replace("%password%", password));
                bot.sendMessage(options.loginCommand().replace("%password%", password));
            }
        } catch (Exception e) {
            log.error("Error while logging join", e);
        }
    }

    public void onLoginDisconnectPacket(String reason) {
        try {
            log.error("Login failed: {}", reason);
        } catch (Exception e) {
            log.error("Error while logging login failed", e);
        }
    }

    public void onDisconnectPacket(String reason) {
        try {
            log.error("Disconnected: {}", reason);
        } catch (Exception e) {
            log.error("Error while logging disconnect", e);
        }
    }

    public void onDisconnectEvent(String reason, Throwable cause) {
        try {
            if (cause == null) { // Packet wise disconnects have no cause
                return;
            }

            if (cause.getClass().getSimpleName().equals("UnexpectedEncryptionException")) {
                log.error("Server is online mode!");
            } else if (reason.contains("Connection refused")) {
                log.error("Server is not reachable!");
            } else {
                log.error("Disconnected: {}", reason);
            }

            if (options.debug()) {
                log.debug("Bot disconnected with cause: ", cause);
            }
        } catch (Exception e) {
            log.error("Error while logging disconnect", e);
        }
    }
}
