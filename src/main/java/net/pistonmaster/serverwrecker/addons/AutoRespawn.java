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

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.GlobalEventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.RandomUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class AutoRespawn implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
        AddonHelper.registerBotEventConsumer(SWPacketReceiveEvent.class, this::onPacket);
    }

    public void onPacket(SWPacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
            if (!event.connection().settingsHolder().has(AutoRespawnSettings.class)) {
                return;
            }

            var autoRespawnSettings = event.connection().settingsHolder().get(AutoRespawnSettings.class);
            if (!autoRespawnSettings.autoRespawn()) {
                return;
            }

            var message = ServerWreckerServer.PLAIN_MESSAGE_SERIALIZER.serialize(combatKillPacket.getMessage());
            event.connection().logger().info("[AutoRespawn] Died with killer: {} and message: '{}'",
                    combatKillPacket.getPlayerId(), message);

            event.connection().executorManager().newScheduledExecutorService("Respawn").schedule(() ->
                            event.connection().session().send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)),
                    RandomUtil.getRandomInt(autoRespawnSettings.minDelay(), autoRespawnSettings.maxDelay()), TimeUnit.SECONDS);
        }
    }

    @GlobalEventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoRespawnPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @GlobalEventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoRespawnSettings.class, new AutoRespawnCommand());
    }

    private static class AutoRespawnPanel extends NavigationItem implements SettingsDuplex<AutoRespawnSettings> {
        private final JCheckBox autoRespawn;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        AutoRespawnPanel(ServerWreckerServer serverWreckerServer) {
            super();
            serverWreckerServer.getSettingsManager().registerDuplex(AutoRespawnSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Respawn?"));
            autoRespawn = new PresetJCheckBox(AutoRespawnSettings.DEFAULT_AUTO_RESPAWN);
            add(autoRespawn);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoRespawnSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoRespawnSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Auto Respawn";
        }

        @Override
        public String getNavigationId() {
            return "auto-respawn";
        }

        @Override
        public void onSettingsChange(AutoRespawnSettings settings) {
            autoRespawn.setSelected(settings.autoRespawn());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoRespawnSettings collectSettings() {
            return new AutoRespawnSettings(
                    autoRespawn.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoRespawnCommand implements SettingsProvider<AutoRespawnSettings> {
        @CommandLine.Option(names = {"--auto-respawn"}, description = "Respawn bots after death")
        private boolean autoRespawn = AutoRespawnSettings.DEFAULT_AUTO_RESPAWN;
        @CommandLine.Option(names = {"--respawn-min-delay"}, description = "Minimum delay between respawns")
        private int minDelay = AutoRespawnSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--respawn-max-delay"}, description = "Maximum delay between respawns")
        private int maxDelay = AutoRespawnSettings.DEFAULT_MAX_DELAY;

        @Override
        public AutoRespawnSettings collectSettings() {
            return new AutoRespawnSettings(
                    autoRespawn,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoRespawnSettings(
            boolean autoRespawn,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_AUTO_RESPAWN = true;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 3;
    }
}
