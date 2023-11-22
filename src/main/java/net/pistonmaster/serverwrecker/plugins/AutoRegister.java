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
package net.pistonmaster.serverwrecker.plugins;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.PluginPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsManagerInitEvent;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.settings.lib.property.StringProperty;

public class AutoRegister implements InternalExtension {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoRegister.class);
        PluginHelper.registerBotEventConsumer(ChatMessageReceiveEvent.class, AutoRegister::onChat);
    }

    public static void onChat(ChatMessageReceiveEvent event) {
        var settingsHolder = event.connection().settingsHolder();
        var plainMessage = event.parseToText();
        if (!autoRegisterSettings.autoRegister()) {
            return;
        }

        var password = autoRegisterSettings.passwordFormat();

        // TODO: Add more password options
        if (plainMessage.contains("/register")) {
            event.connection().botControl()
                    .sendMessage(autoRegisterSettings.registerCommand().replace("%password%", password));
        } else if (plainMessage.contains("/login")) {
            event.connection().botControl()
                    .sendMessage(autoRegisterSettings.loginCommand().replace("%password%", password));
        } else if (plainMessage.contains("/captcha")) {
            var split = plainMessage.split(" ");

            for (var i = 0; i < split.length; i++) {
                if (split[i].equals("/captcha")) {
                    event.connection().botControl()
                            .sendMessage(autoRegisterSettings.captchaCommand().replace("%captcha%", split[i + 1]));
                }
            }
        }
    }

    @EventHandler
    public static void onPluginPanel(SettingsManagerInitEvent event) {
        event.settingsManager().addClass(AutoRegisterSettings.class);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoRegisterSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-register");
        public static final BooleanProperty AUTO_REGISTER = BUILDER.ofBoolean(
                "auto-register",
                "Auto Register: ",
                "Make bots run the /register and /login command after joining",
                new String[]{"--auto-register"},
                false
        );
        public static final StringProperty REGISTER_COMMAND = BUILDER.ofString(
                "register-command",
                "Register Command: ",
                "Command to be executed to register",
                new String[]{"--register-command"},
                "/register %password% %password%"
        );
        public static final StringProperty LOGIN_COMMAND = BUILDER.ofString(
                "login-command",
                "Login Command: ",
                "Command to be executed to log in",
                new String[]{"--login-command"},
                "/login %password%"
        );
        public static final StringProperty CAPTCHA_COMMAND = BUILDER.ofString(
                "captcha-command",
                "Captcha Command: ",
                "Command to be executed to confirm a captcha",
                new String[]{"--captcha-command"},
                "/captcha %captcha%"
        );
        public static final StringProperty PASSWORD_FORMAT = BUILDER.ofString(
                "password-format",
                "Password Format: ",
                "The password for registering",
                new String[]{"--password-format"},
                "ServerWrecker"
        );
    }
}
