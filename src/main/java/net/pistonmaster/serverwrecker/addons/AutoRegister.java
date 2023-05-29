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

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.settings.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;

public class AutoRegister implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onChat(ChatMessageReceiveEvent event) {
        if (!event.connection().settingsHolder().has(AutoRegisterSettings.class)) {
            return;
        }

        AutoRegisterSettings autoRegisterSettings = event.connection().settingsHolder().get(AutoRegisterSettings.class);
        String plainMessage = event.parseToText();
        if (!autoRegisterSettings.autoRegister()) {
            return;
        }

        String password = autoRegisterSettings.passwordFormat();

        // TODO: Add more password options
        if (plainMessage.contains("/register")) {
            event.connection().sendMessage(autoRegisterSettings.registerCommand().replace("%password%", password));
        } else if (plainMessage.contains("/login")) {
            event.connection().sendMessage(autoRegisterSettings.loginCommand().replace("%password%", password));
        } else if (plainMessage.contains("/captcha")) {
            String[] split = plainMessage.split(" ");

            for (int i = 0; i < split.length; i++) {
                if (split[i].equals("/captcha")) {
                    event.connection().sendMessage(autoRegisterSettings.captchaCommand().replace("%captcha%", split[i + 1]));
                }
            }
        }
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoRegisterPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AutoRegisterCommand autoRegisterCommand = new AutoRegisterCommand();
        CommandLine.Model.CommandSpec commandSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(autoRegisterCommand);
        for (CommandLine.Model.OptionSpec optionSpec : commandSpec.options()) {
            event.commandLine().getCommandSpec().addOption(optionSpec);
        }

        ServerWreckerAPI.getServerWrecker().getSettingsManager().registerProvider(AutoRegisterSettings.class, autoRegisterCommand);
    }

    private static class AutoRegisterPanel extends NavigationItem implements SettingsDuplex<AutoRegisterSettings> {
        private final JCheckBox autoRegister;
        private final JTextField registerCommand;
        private final JTextField loginCommand;
        private final JTextField captchaCommand;
        private final JTextField passwordFormat;

        public AutoRegisterPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoRegisterSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Auto Register: "));
            autoRegister = new JCheckBox();
            add(autoRegister);

            add(new JLabel("Register Command: "));
            registerCommand = new JTextField("/register %password% %password%");
            add(registerCommand);

            add(new JLabel("Login Command: "));
            loginCommand = new JTextField("/login %password%");
            add(loginCommand);

            add(new JLabel("Captcha Command: "));
            captchaCommand = new JTextField("/captcha %captcha%");
            add(captchaCommand);

            add(new JLabel("Password Format: "));
            passwordFormat = new JTextField("ServerWrecker");
            add(passwordFormat);
        }

        @Override
        public String getNavigationName() {
            return "Auto Register";
        }

        @Override
        public String getNavigationId() {
            return "auto-register";
        }

        @Override
        public void onSettingsChange(AutoRegisterSettings settings) {
            autoRegister.setSelected(settings.autoRegister());
            registerCommand.setText(settings.registerCommand());
            loginCommand.setText(settings.loginCommand());
            captchaCommand.setText(settings.captchaCommand());
            passwordFormat.setText(settings.passwordFormat());
        }

        @Override
        public AutoRegisterSettings collectSettings() {
            return new AutoRegisterSettings(
                    autoRegister.isSelected(),
                    registerCommand.getText(),
                    loginCommand.getText(),
                    captchaCommand.getText(),
                    passwordFormat.getText()
            );
        }
    }

    private static class AutoRegisterCommand implements SettingsProvider<AutoRegisterSettings> {
        @CommandLine.Option(names = {"--auto-register"}, description = "make bots run the /register and /login command after joining")
        private boolean autoRegister;
        @CommandLine.Option(names = {"--register-command"}, description = "command to be executed to register")
        private String registerCommand = "/register %password% %password%";
        @CommandLine.Option(names = {"--login-command"}, description = "command to be executed to log in")
        private String loginCommand = "/login %password%";
        @CommandLine.Option(names = {"--captcha-command"}, description = "command to be executed to confirm a captcha")
        private String captchaCommand = "/captcha %captcha%";
        @CommandLine.Option(names = {"--password-format"}, description = "the password for registering")
        private String passwordFormat = "ServerWrecker";

        @Override
        public AutoRegisterSettings collectSettings() {
            return new AutoRegisterSettings(
                    autoRegister,
                    registerCommand,
                    loginCommand,
                    captchaCommand,
                    passwordFormat
            );
        }
    }

    private record AutoRegisterSettings(
            boolean autoRegister,
            String registerCommand,
            String loginCommand,
            String captchaCommand,
            String passwordFormat
    ) implements SettingsObject {
    }
}
