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
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
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

public class AutoRespawn implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPacket(SWPacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
            if (!event.getConnection().settingsHolder().has(AutoRespawnSettings.class)) {
                return;
            }

            AutoRespawnSettings autoRespawnSettings = event.getConnection().settingsHolder().get(AutoRespawnSettings.class);
            if (!autoRespawnSettings.autoRespawn()) {
                return;
            }

            String message = event.getConnection().serverWrecker().getMessageSerializer().serialize(combatKillPacket.getMessage());
            event.getConnection().logger().info("[AutoRespawn] Died with killer: {} and message: '{}'",
                    combatKillPacket.getKillerId(), message);

            event.getConnection().serverWrecker().getScheduler().schedule(() -> {
                event.getConnection().session().send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
            }, ThreadLocalRandom.current()
                    .nextInt(autoRespawnSettings.minDelay(), autoRespawnSettings.maxDelay()), TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoRespawnPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoRespawnSettings.class, new AutoRespawnCommand());
    }

    private static class AutoRespawnPanel extends NavigationItem implements SettingsDuplex<AutoRespawnSettings> {
        private final JCheckBox autoRespawn;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        public AutoRespawnPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoRespawnSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Respawn?"));
            autoRespawn = new JCheckBox();
            autoRespawn.setSelected(true);
            add(autoRespawn);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(3, 1, 1000, 1));
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
        @CommandLine.Option(names = {"--auto-respawn"}, description = "respawn bots after death")
        private boolean autoRespawn = true;
        @CommandLine.Option(names = {"--respawn-min-delay"}, description = "minimum delay between respawns")
        private int minDelay = 1;
        @CommandLine.Option(names = {"--respawn-max-delay"}, description = "maximum delay between respawns")
        private int maxDelay = 3;

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
    }
}
