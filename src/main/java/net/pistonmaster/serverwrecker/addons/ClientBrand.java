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
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.settings.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

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

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ClientBrandSettings.class, new ClientBrandCommand());
    }

    private static class ClientBrandPanel extends NavigationItem implements SettingsDuplex<ClientBrandSettings> {
        private final JCheckBox sendClientBrand;
        private final JTextField clientBrand;

        public ClientBrandPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(ClientBrandSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Send Client Brand: "));
            sendClientBrand = new PresetJCheckBox(ClientBrandSettings.DEFAULT_SEND_CLIENT_BRAND);
            add(sendClientBrand);

            add(new JLabel("Client Brand: "));
            clientBrand = new JTextField(ClientBrandSettings.DEFAULT_CLIENT_BRAND);
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

    private static class ClientBrandCommand implements SettingsProvider<ClientBrandSettings> {
        @CommandLine.Option(names = {"--send-client-brand"}, description = "Send client brand")
        private boolean sendClientBrand = ClientBrandSettings.DEFAULT_SEND_CLIENT_BRAND;
        @CommandLine.Option(names = {"--client-brand"}, description = "Client brand")
        private String clientBrand = ClientBrandSettings.DEFAULT_CLIENT_BRAND;

        @Override
        public ClientBrandSettings collectSettings() {
            return new ClientBrandSettings(
                    sendClientBrand,
                    clientBrand
            );
        }
    }

    private record ClientBrandSettings(
            boolean sendClientBrand,
            String clientBrand
    ) implements SettingsObject {
        public static final boolean DEFAULT_SEND_CLIENT_BRAND = true;
        public static final String DEFAULT_CLIENT_BRAND = "vanilla";
    }
}
