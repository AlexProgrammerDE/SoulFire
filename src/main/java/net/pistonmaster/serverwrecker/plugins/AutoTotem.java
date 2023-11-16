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

import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.PluginCLIHelper;
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotJoinedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.PluginPanelInitEvent;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class AutoTotem implements InternalExtension {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoTotem.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoTotem::onJoined);
    }

    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        if (!connection.settingsHolder().has(AutoTotemSettings.class)) {
            return;
        }

        var settings = connection.settingsHolder().get(AutoTotemSettings.class);
        if (!settings.autoTotem()) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService("AutoTotem");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var inventoryManager = sessionDataManager.getInventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();
            var offhandSlot = playerInventory.getOffhand();

            // We only want to use totems if there are no items in the offhand
            if (offhandSlot.item() != null) {
                return;
            }

            for (var slot : playerInventory.getStorage()) {
                if (slot.item() == null) {
                    continue;
                }

                var item = slot.item();
                if (item.getType() == ItemType.TOTEM_OF_UNDYING) {
                    if (!inventoryManager.tryInventoryControl()) {
                        return;
                    }

                    try {
                        inventoryManager.leftClickSlot(slot.slot());
                        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                        inventoryManager.leftClickSlot(offhandSlot.slot());
                    } finally {
                        inventoryManager.unlockInventoryControl();
                    }
                    return;
                }
            }
        }, settings.minDelay(), settings.maxDelay());
    }

    @EventHandler
    public static void onPluginPanel(PluginPanelInitEvent event) {
        event.navigationItems().add(new AutoTotemPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public static void onCommandLine(CommandManagerInitEvent event) {
        PluginCLIHelper.registerCommands(event.commandLine(), AutoTotemSettings.class, new AutoTotemCommand());
    }

    private static class AutoTotemPanel extends NavigationItem implements SettingsDuplex<AutoTotemSettings> {
        private final JCheckBox autoTotem;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        AutoTotemPanel(ServerWreckerServer serverWreckerServer) {
            super();
            serverWreckerServer.getSettingsManager().registerDuplex(AutoTotemSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Totem?"));
            autoTotem = new PresetJCheckBox(AutoTotemSettings.DEFAULT_AUTO_TOTEM);
            add(autoTotem);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoTotemSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoTotemSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Auto Totem";
        }

        @Override
        public String getNavigationId() {
            return "auto-totem";
        }

        @Override
        public void onSettingsChange(AutoTotemSettings settings) {
            autoTotem.setSelected(settings.autoTotem());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoTotemSettings collectSettings() {
            return new AutoTotemSettings(
                    autoTotem.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoTotemCommand implements SettingsProvider<AutoTotemSettings> {
        @CommandLine.Option(names = {"--auto-totem"}, description = "Do auto totem?")
        private boolean autoTotem = AutoTotemSettings.DEFAULT_AUTO_TOTEM;
        @CommandLine.Option(names = {"--totem-min-delay"}, description = "Minimum delay between using totems")
        private int minDelay = AutoTotemSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--totem-max-delay"}, description = "Maximum delay between using totems")
        private int maxDelay = AutoTotemSettings.DEFAULT_MAX_DELAY;

        @Override
        public AutoTotemSettings collectSettings() {
            return new AutoTotemSettings(
                    autoTotem,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoTotemSettings(
            boolean autoTotem,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_AUTO_TOTEM = true;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 2;
    }
}
