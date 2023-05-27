/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.addons;

import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoRegister implements InternalAddon, EventSubscriber<ChatMessageReceiveEvent> {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListener(ChatMessageReceiveEvent.class, this);
    }

    @Override
    public void on(@NonNull ChatMessageReceiveEvent event) {
        BotSettings botSettings = event.connection().settingsHolder().get(BotSettings.class);
        String plainMessage = event.parseToText();
        if (!botSettings.autoRegister()) {
            return;
        }

        String password = botSettings.passwordFormat();

        // TODO: Add more password options
        if (plainMessage.contains("/register")) {
            event.connection().sendMessage(botSettings.registerCommand().replace("%password%", password));
        } else if (plainMessage.contains("/login")) {
            event.connection().sendMessage(botSettings.loginCommand().replace("%password%", password));
        } else if (plainMessage.contains("/captcha")) {
            String[] split = plainMessage.split(" ");

            for (int i = 0; i < split.length; i++) {
                if (split[i].equals("/captcha")) {
                    event.connection().sendMessage(botSettings.captchaCommand().replace("%captcha%", split[i + 1]));
                }
            }
        }
    }
}
