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
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientSettings implements InternalAddon {
    private final ClientSettingsPanel clientSettingsPanel = new ClientSettingsPanel();

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPacket(SWPacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerAbilitiesPacket) { // Recommended packet to use
            if (!clientSettingsPanel.getSendClientSettings().isSelected()) {
                return;
            }

            List<SkinPart> skinParts = new ArrayList<>();
            if (clientSettingsPanel.getCapeEnabled().isSelected()) {
                skinParts.add(SkinPart.CAPE);
            }
            if (clientSettingsPanel.getJacketEnabled().isSelected()) {
                skinParts.add(SkinPart.JACKET);
            }
            if (clientSettingsPanel.getLeftSleeveEnabled().isSelected()) {
                skinParts.add(SkinPart.LEFT_SLEEVE);
            }
            if (clientSettingsPanel.getRightSleeveEnabled().isSelected()) {
                skinParts.add(SkinPart.RIGHT_SLEEVE);
            }
            if (clientSettingsPanel.getLeftPantsLegEnabled().isSelected()) {
                skinParts.add(SkinPart.LEFT_PANTS_LEG);
            }
            if (clientSettingsPanel.getRightPantsLegEnabled().isSelected()) {
                skinParts.add(SkinPart.RIGHT_PANTS_LEG);
            }
            if (clientSettingsPanel.getHatEnabled().isSelected()) {
                skinParts.add(SkinPart.HAT);
            }

            event.getConnection().session().send(new ServerboundClientInformationPacket(
                    clientSettingsPanel.getClientLocale().getText(),
                    (int) clientSettingsPanel.getRenderDistance().getValue(),
                    Objects.requireNonNull(clientSettingsPanel.getChatVisibility().getSelectedEnum()),
                    clientSettingsPanel.getUseChatColors().isSelected(),
                    skinParts,
                    Objects.requireNonNull(clientSettingsPanel.getHandPreference().getSelectedEnum()),
                    clientSettingsPanel.getTextFilteringEnabled().isSelected(),
                    clientSettingsPanel.getAllowsListing().isSelected()
            ));
        }
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(clientSettingsPanel);
    }

    @Getter
    private static class ClientSettingsPanel extends NavigationItem {
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

        public ClientSettingsPanel() {
            super();

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
    }
}
