/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.plugins;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.settings.lib.property.StringProperty;

public class AutoRegister implements InternalExtension {
    public static void onChat(ChatMessageReceiveEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoRegisterSettings.ENABLED)) {
            return;
        }

        var plainMessage = event.parseToText();
        var password = settingsHolder.get(AutoRegisterSettings.PASSWORD_FORMAT);

        // TODO: Add more password options
        if (plainMessage.contains("/register")) {
            var registerCommand = settingsHolder.get(AutoRegisterSettings.REGISTER_COMMAND);
            connection.botControl().sendMessage(registerCommand.replace("%password%", password));
        } else if (plainMessage.contains("/login")) {
            var loginCommand = settingsHolder.get(AutoRegisterSettings.LOGIN_COMMAND);
            connection.botControl().sendMessage(loginCommand.replace("%password%", password));
        } else if (plainMessage.contains("/captcha")) {
            var captchaCommand = settingsHolder.get(AutoRegisterSettings.CAPTCHA_COMMAND);
            var split = plainMessage.split(" ");

            for (var i = 0; i < split.length; i++) {
                if (split[i].equals("/captcha")) {
                    connection.botControl().sendMessage(captchaCommand.replace("%captcha%", split[i + 1]));
                }
            }
        }
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoRegisterSettings.class, "Auto Register");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(AutoRegister.class);
        PluginHelper.registerBotEventConsumer(ChatMessageReceiveEvent.class, AutoRegister::onChat);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoRegisterSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-register");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Enable Auto Register",
                new String[]{"--auto-register"},
                "Make bots run the /register and /login command after joining",
                false
        );
        public static final StringProperty REGISTER_COMMAND = BUILDER.ofString(
                "register-command",
                "Register Command",
                new String[]{"--register-command"},
                "Command to be executed to register",
                "/register %password% %password%"
        );
        public static final StringProperty LOGIN_COMMAND = BUILDER.ofString(
                "login-command",
                "Login Command",
                new String[]{"--login-command"},
                "Command to be executed to log in",
                "/login %password%"
        );
        public static final StringProperty CAPTCHA_COMMAND = BUILDER.ofString(
                "captcha-command",
                "Captcha Command",
                new String[]{"--captcha-command"},
                "Command to be executed to confirm a captcha",
                "/captcha %captcha%"
        );
        public static final StringProperty PASSWORD_FORMAT = BUILDER.ofString(
                "password-format",
                "Password Format",
                new String[]{"--password-format"},
                "The password for registering",
                "SoulFire"
        );
    }
}
