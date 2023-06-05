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
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.UnregisterCleanup;
import net.pistonmaster.serverwrecker.api.event.bot.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AutoJump implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPreConnect(PreBotConnectEvent event) {
        event.connection().cleanup(new BotJumpThread(event.connection(),
                Executors.newScheduledThreadPool(1), new LinkedHashSet<>()));
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoJumpPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoJumpSettings.class, new AutoJumpCommand());
    }

    private record BotJumpThread(BotConnection connection, ScheduledExecutorService executor,
                                 Set<String> messageQueue) implements UnregisterCleanup {
        public BotJumpThread {
            if (connection.settingsHolder().has(AutoJumpSettings.class)) {
                AutoJumpSettings settings = connection.settingsHolder().get(AutoJumpSettings.class);

                ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
                    SessionDataManager sessionDataManager = connection.sessionDataManager();
                    LevelState level = sessionDataManager.getCurrentLevel();
                    BotMovementManager movementManager = sessionDataManager.getBotMovementManager();
                    if (level != null && movementManager != null
                            && level.isChunkLoaded(movementManager.getBlockPos())) {
                        connection.logger().info("[AutoJump] Jumping!");
                        movementManager.jump();
                    }
                }, settings.minDelay(), settings.maxDelay());
            }
        }

        @Override
        public void cleanup() {
            executor.shutdown();
        }
    }

    private static class AutoJumpPanel extends NavigationItem implements SettingsDuplex<AutoJumpSettings> {
        private final JCheckBox autoJump;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        public AutoJumpPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoJumpSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Jump?"));
            autoJump = new PresetJCheckBox(AutoJumpSettings.DEFAULT_AUTO_JUMP);
            add(autoJump);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoJumpSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoJumpSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Auto Jump";
        }

        @Override
        public String getNavigationId() {
            return "auto-jump";
        }

        @Override
        public void onSettingsChange(AutoJumpSettings settings) {
            autoJump.setSelected(settings.autoJump());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoJumpSettings collectSettings() {
            return new AutoJumpSettings(
                    autoJump.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoJumpCommand implements SettingsProvider<AutoJumpSettings> {
        @CommandLine.Option(names = {"--auto-jump"}, description = "Do auto jump?")
        private boolean autoJump = AutoJumpSettings.DEFAULT_AUTO_JUMP;
        @CommandLine.Option(names = {"--jump-min-delay"}, description = "Minimum delay between jumps")
        private int minDelay = AutoJumpSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--jump-max-delay"}, description = "Maximum delay between jumps")
        private int maxDelay = AutoJumpSettings.DEFAULT_MAX_DELAY;

        @Override
        public AutoJumpSettings collectSettings() {
            return new AutoJumpSettings(
                    autoJump,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoJumpSettings(
            boolean autoJump,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_AUTO_JUMP = false;
        public static final int DEFAULT_MIN_DELAY = 2;
        public static final int DEFAULT_MAX_DELAY = 5;
    }
}
