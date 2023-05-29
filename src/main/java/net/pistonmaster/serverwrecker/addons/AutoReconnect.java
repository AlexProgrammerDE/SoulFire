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
import net.pistonmaster.serverwrecker.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.settings.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AutoReconnect implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onDisconnect(BotDisconnectedEvent event) {
        if (!event.connection().settingsHolder().has(AutoReconnectSettings.class)) {
            return;
        }

        AutoReconnectSettings autoReconnectSettings = event.connection().settingsHolder().get(AutoReconnectSettings.class);
        if (!autoReconnectSettings.autoReconnect() || event.connection().serverWrecker().getAttackState().isInactive()) {
            return;
        }

        event.connection().serverWrecker().getScheduler().schedule(() -> {
            event.connection().factory().connect()
                    .thenAccept(newConnection -> event.connection().serverWrecker().getBotConnections()
                            .replaceAll(connection1 -> connection1 == event.connection() ? newConnection : connection1));
        }, ThreadLocalRandom.current()
                .nextInt(autoReconnectSettings.minDelay(), autoReconnectSettings.maxDelay()), TimeUnit.SECONDS);
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoReconnectPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AutoReconnectCommand autoReconnectCommand = new AutoReconnectCommand();
        CommandLine.Model.CommandSpec commandSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(autoReconnectCommand);
        for (CommandLine.Model.OptionSpec optionSpec : commandSpec.options()) {
            event.commandLine().getCommandSpec().addOption(optionSpec);
        }

        ServerWreckerAPI.getServerWrecker().getSettingsManager().registerProvider(AutoReconnectSettings.class, autoReconnectCommand);
    }

    private static class AutoReconnectPanel extends NavigationItem implements SettingsDuplex<AutoReconnectSettings> {
        private final JCheckBox autoReconnect;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        public AutoReconnectPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoReconnectSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Reconnect?"));
            autoReconnect = new JCheckBox();
            autoReconnect.setSelected(true);
            add(autoReconnect);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(5, 1, 1000, 1));
            add(maxDelay);

            minDelay.addChangeListener(e -> {
                if ((int) minDelay.getValue() > (int) maxDelay.getValue()) {
                    maxDelay.setValue(minDelay.getValue());
                }
            });
            maxDelay.addChangeListener(e -> {
                if ((int) minDelay.getValue() > (int) maxDelay.getValue()) {
                    minDelay.setValue(maxDelay.getValue());
                }
            });
        }

        @Override
        public String getNavigationName() {
            return "Auto Reconnect";
        }

        @Override
        public String getNavigationId() {
            return "auto-reconnect";
        }

        @Override
        public void onSettingsChange(AutoReconnectSettings settings) {
            autoReconnect.setSelected(settings.autoReconnect());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoReconnectSettings collectSettings() {
            return new AutoReconnectSettings(
                    autoReconnect.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoReconnectCommand implements SettingsProvider<AutoReconnectSettings> {
        @CommandLine.Option(names = {"--auto-reconnect"}, description = "reconnect bots after being disconnected")
        private boolean autoReconnect = true;
        @CommandLine.Option(names = {"--reconnect-min-delay"}, description = "minimum delay between reconnects")
        private int minDelay = 1;
        @CommandLine.Option(names = {"--reconnect-max-delay"}, description = "maximum delay between reconnects")
        private int maxDelay = 5;

        @Override
        public AutoReconnectSettings collectSettings() {
            return new AutoReconnectSettings(
                    autoReconnect,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoReconnectSettings(
            boolean autoReconnect,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
    }
}
