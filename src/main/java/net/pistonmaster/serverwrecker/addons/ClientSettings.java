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
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.settings.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.settings.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

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

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ClientSettingsSettings.class, new ClientSettingsCommand());
    }

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
            sendClientSettings = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_SEND_CLIENT_SETTINGS);
            add(sendClientSettings);

            add(new JLabel("Client Locale: "));
            clientLocale = new JTextField(ClientSettingsSettings.DEFAULT_CLIENT_LOCALE);
            add(clientLocale);

            add(new JLabel("Render Distance: "));
            renderDistance = new JSpinner(
                    new SpinnerNumberModel(ClientSettingsSettings.DEFAULT_RENDER_DISTANCE, 2, 32, 1));
            add(renderDistance);

            add(new JLabel("Chat Visibility: "));
            chatVisibility = new JEnumComboBox<>(ChatVisibility.class, ClientSettingsSettings.DEFAULT_CHAT_VISIBILITY);
            add(chatVisibility);

            add(new JLabel("Use Chat Colors: "));
            useChatColors = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_USE_CHAT_COLORS);
            add(useChatColors);

            add(new JLabel("Cape Enabled: "));
            capeEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_CAPE_ENABLED);
            add(capeEnabled);

            add(new JLabel("Jacket Enabled: "));
            jacketEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_JACKET_ENABLED);
            add(jacketEnabled);

            add(new JLabel("Left Sleeve Enabled: "));
            leftSleeveEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_LEFT_SLEEVE_ENABLED);
            add(leftSleeveEnabled);

            add(new JLabel("Right Sleeve Enabled: "));
            rightSleeveEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_RIGHT_SLEEVE_ENABLED);
            add(rightSleeveEnabled);

            add(new JLabel("Left Pants Leg Enabled: "));
            leftPantsLegEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_LEFT_PANTS_LEG_ENABLED);
            add(leftPantsLegEnabled);

            add(new JLabel("Right Pants Leg Enabled: "));
            rightPantsLegEnabled = new PresetJCheckBox( ClientSettingsSettings.DEFAULT_RIGHT_PANTS_LEG_ENABLED);
            add(rightPantsLegEnabled);

            add(new JLabel("Hat Enabled: "));
            hatEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_HAT_ENABLED);
            add(hatEnabled);

            add(new JLabel("Hand Preference: "));
            handPreference = new JEnumComboBox<>(HandPreference.class, ClientSettingsSettings.DEFAULT_HAND_PREFERENCE);
            add(handPreference);

            add(new JLabel("Text Filtering Enabled: "));
            textFilteringEnabled = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_TEXT_FILTERING_ENABLED);
            add(textFilteringEnabled);

            add(new JLabel("Allows Listing: "));
            allowsListing = new PresetJCheckBox(ClientSettingsSettings.DEFAULT_ALLOWS_LISTING);
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

    private static class ClientSettingsCommand implements SettingsProvider<ClientSettingsSettings> {
        @CommandLine.Option(names = {"--send-client-settings"} , description = "Send client settings")
        private boolean sendClientBrand = ClientSettingsSettings.DEFAULT_SEND_CLIENT_SETTINGS;
        @CommandLine.Option(names = {"--client-locale"} , description = "Client locale")
        private String clientBrand = ClientSettingsSettings.DEFAULT_CLIENT_LOCALE;
        @CommandLine.Option(names = {"--render-distance"} , description = "Render distance")
        private int renderDistance = ClientSettingsSettings.DEFAULT_RENDER_DISTANCE;
        @CommandLine.Option(names = {"--chat-visibility"} , description = "Chat visibility")
        private ChatVisibility chatVisibility = ClientSettingsSettings.DEFAULT_CHAT_VISIBILITY;
        @CommandLine.Option(names = {"--use-chat-colors"} , description = "Use chat colors")
        private boolean useChatColors = ClientSettingsSettings.DEFAULT_USE_CHAT_COLORS;
        @CommandLine.Option(names = {"--cape-enabled"} , description = "Cape enabled")
        private boolean capeEnabled = ClientSettingsSettings.DEFAULT_CAPE_ENABLED;
        @CommandLine.Option(names = {"--jacket-enabled"} , description = "Jacket enabled")
        private boolean jacketEnabled = ClientSettingsSettings.DEFAULT_JACKET_ENABLED;
        @CommandLine.Option(names = {"--left-sleeve-enabled"} , description = "Left sleeve enabled")
        private boolean leftSleeveEnabled = ClientSettingsSettings.DEFAULT_LEFT_SLEEVE_ENABLED;
        @CommandLine.Option(names = {"--right-sleeve-enabled"} , description = "Right sleeve enabled")
        private boolean rightSleeveEnabled = ClientSettingsSettings.DEFAULT_RIGHT_SLEEVE_ENABLED;
        @CommandLine.Option(names = {"--left-pants-leg-enabled"} , description = "Left pants leg enabled")
        private boolean leftPantsLegEnabled = ClientSettingsSettings.DEFAULT_LEFT_PANTS_LEG_ENABLED;
        @CommandLine.Option(names = {"--right-pants-leg-enabled"} , description = "Right pants leg enabled")
        private boolean rightPantsLegEnabled = ClientSettingsSettings.DEFAULT_RIGHT_PANTS_LEG_ENABLED;
        @CommandLine.Option(names = {"--hat-enabled"} , description = "Hat enabled")
        private boolean hatEnabled = ClientSettingsSettings.DEFAULT_HAT_ENABLED;
        @CommandLine.Option(names = {"--hand-preference"} , description = "Hand preference")
        private HandPreference handPreference = ClientSettingsSettings.DEFAULT_HAND_PREFERENCE;
        @CommandLine.Option(names = {"--text-filtering-enabled"} , description = "Text filtering enabled")
        private boolean textFilteringEnabled = ClientSettingsSettings.DEFAULT_TEXT_FILTERING_ENABLED;
        @CommandLine.Option(names = {"--allows-listing"} , description = "Allows listing")
        private boolean allowsListing = ClientSettingsSettings.DEFAULT_ALLOWS_LISTING;

        @Override
        public ClientSettingsSettings collectSettings() {
            return new ClientSettingsSettings(
                    sendClientBrand,
                    clientBrand,
                    renderDistance,
                    chatVisibility,
                    useChatColors,
                    capeEnabled,
                    jacketEnabled,
                    leftSleeveEnabled,
                    rightSleeveEnabled,
                    leftPantsLegEnabled,
                    rightPantsLegEnabled,
                    hatEnabled,
                    handPreference,
                    textFilteringEnabled,
                    allowsListing
            );
        }
    }

    private record ClientSettingsSettings(
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
        public static final boolean DEFAULT_SEND_CLIENT_SETTINGS = true;
        public static final String DEFAULT_CLIENT_LOCALE = "en_US";
        public static final int DEFAULT_RENDER_DISTANCE = 8;
        public static final ChatVisibility DEFAULT_CHAT_VISIBILITY = ChatVisibility.FULL;
        public static final boolean DEFAULT_USE_CHAT_COLORS = true;
        public static final boolean DEFAULT_CAPE_ENABLED = true;
        public static final boolean DEFAULT_JACKET_ENABLED = true;
        public static final boolean DEFAULT_LEFT_SLEEVE_ENABLED = true;
        public static final boolean DEFAULT_RIGHT_SLEEVE_ENABLED = true;
        public static final boolean DEFAULT_LEFT_PANTS_LEG_ENABLED = true;
        public static final boolean DEFAULT_RIGHT_PANTS_LEG_ENABLED = true;
        public static final boolean DEFAULT_HAT_ENABLED = true;
        public static final HandPreference DEFAULT_HAND_PREFERENCE = HandPreference.RIGHT_HAND;
        public static final boolean DEFAULT_TEXT_FILTERING_ENABLED = false;
        public static final boolean DEFAULT_ALLOWS_LISTING = true;
    }
}
