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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import lombok.Getter;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class ClientBrand implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPacket(SWPacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerAbilitiesPacket) { // Recommended packet to use
            if (!event.getConnection().settingsHolder().has(ClientBrandSettings.class)) {
                return;
            }

            ClientBrandSettings clientBrandSettings = event.getConnection().settingsHolder().get(ClientBrandSettings.class);

            if (!clientBrandSettings.sendClientBrand()) {
                return;
            }

            event.getConnection().session().send(new ServerboundCustomPayloadPacket(
                    "minecraft:brand",
                    clientBrandSettings.clientBrand().getBytes(StandardCharsets.UTF_8)
            ));
        }
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ClientBrandPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @Getter
    private static class ClientBrandPanel extends NavigationItem implements SettingsDuplex<ClientBrandSettings> {
        private final JCheckBox sendClientBrand;
        private final JTextField clientBrand;

        public ClientBrandPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(ClientBrandSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Send Client Brand: "));
            sendClientBrand = new JCheckBox();
            sendClientBrand.setSelected(true);
            add(sendClientBrand);

            add(new JLabel("Client Brand: "));
            clientBrand = new JTextField("vanilla");
            add(clientBrand);
        }

        @Override
        public String getNavigationName() {
            return "Client Brand";
        }

        @Override
        public String getNavigationId() {
            return "client-brand";
        }

        @Override
        public void onSettingsChange(ClientBrandSettings settings) {
            sendClientBrand.setSelected(settings.sendClientBrand());
            clientBrand.setText(settings.clientBrand());
        }

        @Override
        public ClientBrandSettings collectSettings() {
            return new ClientBrandSettings(
                    sendClientBrand.isSelected(),
                    clientBrand.getText()
            );
        }
    }

    private record ClientBrandSettings(
            boolean sendClientBrand,
            String clientBrand
    ) implements SettingsObject {
    }
}
