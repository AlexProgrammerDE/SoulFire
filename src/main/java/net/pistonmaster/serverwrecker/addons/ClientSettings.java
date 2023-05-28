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

import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettings implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPacket(SWPacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerAbilitiesPacket) { // Recommended packet to use
            if (!event.getConnection().settingsHolder().has(ClientSettingsSettings.class)) {
                return;
            }

            ClientSettingsSettings settings = event.getConnection().settingsHolder().get(ClientSettingsSettings.class);
            if (!settings.sendClientSettings()) {
                return;
            }

            List<SkinPart> skinParts = new ArrayList<>();
            if (settings.capeEnabled()) {
                skinParts.add(SkinPart.CAPE);
            }
            if (settings.jacketEnabled()) {
                skinParts.add(SkinPart.JACKET);
            }
            if (settings.leftSleeveEnabled()) {
                skinParts.add(SkinPart.LEFT_SLEEVE);
            }
            if (settings.rightSleeveEnabled()) {
                skinParts.add(SkinPart.RIGHT_SLEEVE);
            }
            if (settings.leftPantsLegEnabled()) {
                skinParts.add(SkinPart.LEFT_PANTS_LEG);
            }
            if (settings.rightPantsLegEnabled()) {
                skinParts.add(SkinPart.RIGHT_PANTS_LEG);
            }
            if (settings.hatEnabled()) {
                skinParts.add(SkinPart.HAT);
            }

            event.getConnection().session().send(new ServerboundClientInformationPacket(
                    settings.clientLocale(),
                    settings.renderDistance(),
                    settings.chatVisibility(),
                    settings.useChatColors(),
                    skinParts,
                    settings.handPreference(),
                    settings.textFilteringEnabled(),
                    settings.allowsListing()
            ));
        }
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ClientSettingsPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @Getter
    private static class ClientSettingsPanel extends NavigationItem implements SettingsDuplex<ClientSettingsSettings> {
        private final JCheckBox sendClientSettings;
        private final JTextField clientLocale;
        private final JSpinner renderDistance;
        private final JEnumComboBox<ChatVisibility> chatVisibility;
        private final JCheckBox useChatColors;
        private final JCheckBox capeEnabled;
        private final JCheckBox jacketEnabled;
        private final JCheckBox leftSleeveEnabled;
        private final JCheckBox rightSleeveEnabled;
        private final JCheckBox leftPantsLegEnabled;
        private final JCheckBox rightPantsLegEnabled;
        private final JCheckBox hatEnabled;
        private final JEnumComboBox<HandPreference> handPreference;
        private final JCheckBox textFilteringEnabled;
        private final JCheckBox allowsListing;

        public ClientSettingsPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(ClientSettingsSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Send Client Settings: "));
            sendClientSettings = new JCheckBox();
            sendClientSettings.setSelected(true);
            add(sendClientSettings);

            add(new JLabel("Client Locale: "));
            clientLocale = new JTextField("en_US");
            add(clientLocale);

            add(new JLabel("Render Distance: "));
            renderDistance = new JSpinner(new SpinnerNumberModel(8, 2, 32, 1));
            add(renderDistance);

            add(new JLabel("Chat Visibility: "));
            chatVisibility = new JEnumComboBox<>(ChatVisibility.class, ChatVisibility.FULL);
            chatVisibility.setSelectedItem(ChatVisibility.FULL);
            add(chatVisibility);

            add(new JLabel("Use Chat Colors: "));
            useChatColors = new JCheckBox();
            useChatColors.setSelected(true);
            add(useChatColors);

            add(new JLabel("Cape Enabled: "));
            capeEnabled = new JCheckBox();
            capeEnabled.setSelected(true);
            add(capeEnabled);

            add(new JLabel("Jacket Enabled: "));
            jacketEnabled = new JCheckBox();
            jacketEnabled.setSelected(true);
            add(jacketEnabled);

            add(new JLabel("Left Sleeve Enabled: "));
            leftSleeveEnabled = new JCheckBox();
            leftSleeveEnabled.setSelected(true);
            add(leftSleeveEnabled);

            add(new JLabel("Right Sleeve Enabled: "));
            rightSleeveEnabled = new JCheckBox();
            rightSleeveEnabled.setSelected(true);
            add(rightSleeveEnabled);

            add(new JLabel("Left Pants Leg Enabled: "));
            leftPantsLegEnabled = new JCheckBox();
            leftPantsLegEnabled.setSelected(true);
            add(leftPantsLegEnabled);

            add(new JLabel("Right Pants Leg Enabled: "));
            rightPantsLegEnabled = new JCheckBox();
            rightPantsLegEnabled.setSelected(true);
            add(rightPantsLegEnabled);

            add(new JLabel("Hat Enabled: "));
            hatEnabled = new JCheckBox();
            hatEnabled.setSelected(true);
            add(hatEnabled);

            add(new JLabel("Hand Preference: "));
            handPreference = new JEnumComboBox<>(HandPreference.class, HandPreference.RIGHT_HAND);
            add(handPreference);

            add(new JLabel("Text Filtering Enabled: "));
            textFilteringEnabled = new JCheckBox();
            textFilteringEnabled.setSelected(false);
            add(textFilteringEnabled);

            add(new JLabel("Allows Listing: "));
            allowsListing = new JCheckBox();
            allowsListing.setSelected(true);
            add(allowsListing);
        }

        @Override
        public String getNavigationName() {
            return "Client Settings";
        }

        @Override
        public String getNavigationId() {
            return "client-settings";
        }

        @Override
        public void onSettingsChange(ClientSettingsSettings settings) {
            sendClientSettings.setSelected(settings.sendClientSettings());
            clientLocale.setText(settings.clientLocale());
            renderDistance.setValue(settings.renderDistance());
            chatVisibility.setSelectedItem(settings.chatVisibility());
            useChatColors.setSelected(settings.useChatColors());
            capeEnabled.setSelected(settings.capeEnabled());
            jacketEnabled.setSelected(settings.jacketEnabled());
            leftSleeveEnabled.setSelected(settings.leftSleeveEnabled());
            rightSleeveEnabled.setSelected(settings.rightSleeveEnabled());
            leftPantsLegEnabled.setSelected(settings.leftPantsLegEnabled());
            rightPantsLegEnabled.setSelected(settings.rightPantsLegEnabled());
            hatEnabled.setSelected(settings.hatEnabled());
            handPreference.setSelectedItem(settings.handPreference());
            textFilteringEnabled.setSelected(settings.textFilteringEnabled());
            allowsListing.setSelected(settings.allowsListing());
        }

        @Override
        public ClientSettingsSettings collectSettings() {
            return new ClientSettingsSettings(
                    sendClientSettings.isSelected(),
                    clientLocale.getText(),
                    (int) renderDistance.getValue(),
                    Objects.requireNonNull(chatVisibility.getSelectedEnum()),
                    useChatColors.isSelected(),
                    capeEnabled.isSelected(),
                    jacketEnabled.isSelected(),
                    leftSleeveEnabled.isSelected(),
                    rightSleeveEnabled.isSelected(),
                    leftPantsLegEnabled.isSelected(),
                    rightPantsLegEnabled.isSelected(),
                    hatEnabled.isSelected(),
                    Objects.requireNonNull(handPreference.getSelectedEnum()),
                    textFilteringEnabled.isSelected(),
                    allowsListing.isSelected()
            );
        }
    }

    private static record ClientSettingsSettings(
            boolean sendClientSettings,
            String clientLocale,
            int renderDistance,
            ChatVisibility chatVisibility,
            boolean useChatColors,
            boolean capeEnabled,
            boolean jacketEnabled,
            boolean leftSleeveEnabled,
            boolean rightSleeveEnabled,
            boolean leftPantsLegEnabled,
            boolean rightPantsLegEnabled,
            boolean hatEnabled,
            HandPreference handPreference,
            boolean textFilteringEnabled,
            boolean allowsListing
    ) implements SettingsObject {
    }
}
