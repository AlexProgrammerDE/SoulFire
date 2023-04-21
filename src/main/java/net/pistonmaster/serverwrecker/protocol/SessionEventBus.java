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
package net.pistonmaster.serverwrecker.protocol;

import lombok.Getter;
import lombok.ToString;
import net.pistonmaster.serverwrecker.common.EntityLocation;
import net.pistonmaster.serverwrecker.common.EntityMotion;
import net.pistonmaster.serverwrecker.common.GameMode;
import net.pistonmaster.serverwrecker.common.SWOptions;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
public final class SessionEventBus {
    private final SWOptions options;
    private final Logger log;
    private final Bot bot;
    private final Set<String> messageQueue = new LinkedHashSet<>();
    private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(3);
    private final AtomicBoolean isRejoining = new AtomicBoolean(false);
    private final AtomicBoolean didFirstJoin = new AtomicBoolean(false);
    private final AtomicInteger rejoinAnywayCounter = new AtomicInteger(0);

    public SessionEventBus(SWOptions options, Logger log, Bot bot) {
        this.options = options;
        this.log = log;
        this.bot = bot;
        timer.scheduleAtFixedRate(() -> {
            Iterator<String> iter = messageQueue.iterator();
            while (!messageQueue.isEmpty()) {
                String message = iter.next();
                iter.remove();
                if (Objects.nonNull(message)) {
                    log.info("Received Message: {}", message);
                }
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(() -> {
            if (options.autoRespawn()) {
                if (bot.isOnline() && bot.getHealth() != -1 && bot.getHealth() < 1) {
                    bot.sendClientCommand(0);
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(() -> {
            if (options.autoReconnect()) {
                if (didFirstJoin.get()
                        || rejoinAnywayCounter.getAndIncrement() > (options.connectTimeout() / 1000)) {
                    if (bot.isOnline()) {
                        if (isRejoining.get()) {
                            isRejoining.set(false);
                        }
                    } else {
                        if (isRejoining.get()) {
                            return;
                        }

                        bot.connect(options.hostname(), options.port(), this);
                        isRejoining.set(true);
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void onChat(String message) {
        try {
            messageQueue.add(message);
            if (options.autoRegister()) {
                String password = options.passwordFormat();

                // TODO: Add more password options
                if (message.contains("/register")) {
                    sendMessage(options.registerCommand().replace("%password%", password));
                } else if (message.contains("/login")) {
                    sendMessage(options.loginCommand().replace("%password%", password));
                } else if (message.contains("/captcha")) {
                    String[] split = message.split(" ");

                    for (int i = 0; i < split.length; i++) {
                        if (split[i].equals("/captcha")) {
                            sendMessage(options.captchaCommand().replace("%captcha%", split[i + 1]));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while logging message", e);
        }
    }

    public void onPosition(double x, double y, double z, float yaw, float pitch) {
        try {
            bot.setLocation(new EntityLocation(x, y, z, yaw, pitch));
            log.info("Position updated: {}", bot.getLocation());
        } catch (Exception e) {
            log.error("Error while logging position", e);
        }
    }

    public void onHealth(float health, int food, float saturation) {
        try {
            bot.setHealth(health);
            bot.setFood(food);
            bot.setSaturation(saturation);
        } catch (Exception e) {
            log.error("Error while logging health", e);
        }
    }

    public void onJoin(int entityId, boolean hardcore, GameMode gameMode, int maxPlayers) {
        try {
            didFirstJoin.set(true);
            bot.setBotEntityId(entityId);
            bot.setHardcore(hardcore);
            bot.setGameMode(gameMode);
            bot.setMaxPlayers(maxPlayers);

            log.info("Joined server");
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
                log.debug("Bot disconnected with cause: ");
                cause.printStackTrace();
            }
        } catch (Exception e) {
            log.error("Error while logging disconnect", e);
        }
    }

    public void onEntityMotion(int entityId, double motionX, double motionY, double motionZ) {
        try {
            if (entityId == bot.getBotEntityId()) {
                bot.setMotion(new EntityMotion(motionX, motionY, motionZ));
                //log.info("Player moved with motion: {} {} {}", motionX, motionY, motionZ);
            } else {
                //log.debug("Entity {} moved with motion: {} {} {}", entityId, motionX, motionY, motionZ);
            }
        } catch (Exception e) {
            log.error("Error while logging entity motion", e);
        }
    }

    private void sendMessage(String message) {
        log.info("Sending Message: {}", message);
        bot.sendMessage(message);
    }
}
