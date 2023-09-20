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

import io.netty.channel.EventLoopGroup;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.GlobalEventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.RandomUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoReconnect implements InternalAddon {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
        AddonHelper.registerBotEventConsumer(BotDisconnectedEvent.class, this::onDisconnect);
    }

    public void onDisconnect(BotDisconnectedEvent event) {
        BotConnection connection = event.connection();
        if (!connection.settingsHolder().has(AutoReconnectSettings.class)) {
            return;
        }

        AutoReconnectSettings autoReconnectSettings = connection.settingsHolder().get(AutoReconnectSettings.class);
        if (!autoReconnectSettings.autoReconnect() || connection.attackManager().getAttackState().isInactive()) {
            return;
        }

        scheduler.schedule(() -> {
            EventLoopGroup eventLoopGroup = connection.session().getEventLoopGroup();
            if (eventLoopGroup.isShuttingDown() || eventLoopGroup.isShutdown() || eventLoopGroup.isTerminated()) {
                return;
            }

            connection.gracefulDisconnect().join();
            BotConnection newConnection = connection.factory().prepareConnection();

            connection.attackManager().getBotConnections()
                    .replaceAll(connectionEntry -> connectionEntry == connection ? newConnection : connectionEntry);

            newConnection.connect();
        }, RandomUtil.getRandomInt(autoReconnectSettings.minDelay(), autoReconnectSettings.maxDelay()), TimeUnit.SECONDS);
    }

    @GlobalEventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoReconnectPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @GlobalEventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoReconnectSettings.class, new AutoReconnectCommand());
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
            autoReconnect = new PresetJCheckBox(AutoReconnectSettings.DEFAULT_AUTO_RECONNECT);
            add(autoReconnect);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoReconnectSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoReconnectSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
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
        @CommandLine.Option(names = {"--auto-reconnect"}, description = "Reconnect bots after being disconnected")
        private boolean autoReconnect = AutoReconnectSettings.DEFAULT_AUTO_RECONNECT;
        @CommandLine.Option(names = {"--reconnect-min-delay"}, description = "Minimum delay between reconnects")
        private int minDelay = AutoReconnectSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--reconnect-max-delay"}, description = "Maximum delay between reconnects")
        private int maxDelay = AutoReconnectSettings.DEFAULT_MAX_DELAY;

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
        public static final boolean DEFAULT_AUTO_RECONNECT = true;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 5;
    }
}
