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
package net.pistonmaster.serverwrecker.server.plugins;

import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.server.api.PluginHelper;
import net.pistonmaster.serverwrecker.server.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.server.api.event.bot.SWPacketSentEvent;
import net.pistonmaster.serverwrecker.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.*;

import javax.inject.Inject;
import java.util.ArrayList;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettings implements InternalExtension {
    public static void onPacket(SWPacketSentEvent event) {
        if (event.packet() instanceof ServerboundLoginAcknowledgedPacket) {
            var connection = event.connection();
            var settingsHolder = connection.settingsHolder();
            if (!settingsHolder.get(ClientSettingsSettings.SEND_CLIENT_SETTINGS)) {
                return;
            }

            var skinParts = new ArrayList<SkinPart>();
            if (settingsHolder.get(ClientSettingsSettings.CAPE_ENABLED)) {
                skinParts.add(SkinPart.CAPE);
            }
            if (settingsHolder.get(ClientSettingsSettings.JACKET_ENABLED)) {
                skinParts.add(SkinPart.JACKET);
            }
            if (settingsHolder.get(ClientSettingsSettings.LEFT_SLEEVE_ENABLED)) {
                skinParts.add(SkinPart.LEFT_SLEEVE);
            }
            if (settingsHolder.get(ClientSettingsSettings.RIGHT_SLEEVE_ENABLED)) {
                skinParts.add(SkinPart.RIGHT_SLEEVE);
            }
            if (settingsHolder.get(ClientSettingsSettings.LEFT_PANTS_LEG_ENABLED)) {
                skinParts.add(SkinPart.LEFT_PANTS_LEG);
            }
            if (settingsHolder.get(ClientSettingsSettings.RIGHT_PANTS_LEG_ENABLED)) {
                skinParts.add(SkinPart.RIGHT_PANTS_LEG);
            }
            if (settingsHolder.get(ClientSettingsSettings.HAT_ENABLED)) {
                skinParts.add(SkinPart.HAT);
            }

            event.connection().session().send(new ServerboundClientInformationPacket(
                    settingsHolder.get(ClientSettingsSettings.CLIENT_LOCALE),
                    settingsHolder.get(ClientSettingsSettings.RENDER_DISTANCE),
                    settingsHolder.get(ClientSettingsSettings.CHAT_VISIBILITY, ChatVisibility.class),
                    settingsHolder.get(ClientSettingsSettings.USE_CHAT_COLORS),
                    skinParts,
                    settingsHolder.get(ClientSettingsSettings.HAND_PREFERENCE, HandPreference.class),
                    settingsHolder.get(ClientSettingsSettings.TEXT_FILTERING_ENABLED),
                    settingsHolder.get(ClientSettingsSettings.ALLOWS_LISTING)
            ));
        }
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ClientSettingsSettings.class, "Client Settings");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(ClientSettings.class);
        PluginHelper.registerBotEventConsumer(SWPacketSentEvent.class, ClientSettings::onPacket);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class ClientSettingsSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("client-settings");
        public static final BooleanProperty SEND_CLIENT_SETTINGS = BUILDER.ofBoolean("send-client-settings",
                "Send client settings",
                "Send client settings",
                new String[]{"--send-client-settings"},
                true
        );
        public static final StringProperty CLIENT_LOCALE = BUILDER.ofString("client-locale",
                "Client locale",
                "Client locale",
                new String[]{"--client-locale"},
                "en_gb"
        );
        public static final IntProperty RENDER_DISTANCE = BUILDER.ofInt("render-distance",
                "Render distance",
                "Render distance",
                new String[]{"--render-distance"},
                8,
                2,
                32,
                1
        );
        public static final ComboProperty CHAT_VISIBILITY = BUILDER.ofEnum("chat-visibility",
                "Chat visibility",
                "Chat visibility",
                new String[]{"--chat-visibility"},
                ChatVisibility.values(),
                ChatVisibility.FULL
        );
        public static final BooleanProperty USE_CHAT_COLORS = BUILDER.ofBoolean("use-chat-colors",
                "Use chat colors",
                "Use chat colors",
                new String[]{"--use-chat-colors"},
                true
        );
        public static final BooleanProperty CAPE_ENABLED = BUILDER.ofBoolean("cape-enabled",
                "Cape enabled",
                "Cape enabled",
                new String[]{"--cape-enabled"},
                true
        );
        public static final BooleanProperty JACKET_ENABLED = BUILDER.ofBoolean("jacket-enabled",
                "Jacket enabled",
                "Jacket enabled",
                new String[]{"--jacket-enabled"},
                true
        );
        public static final BooleanProperty LEFT_SLEEVE_ENABLED = BUILDER.ofBoolean("left-sleeve-enabled",
                "Left sleeve enabled",
                "Left sleeve enabled",
                new String[]{"--left-sleeve-enabled"},
                true
        );
        public static final BooleanProperty RIGHT_SLEEVE_ENABLED = BUILDER.ofBoolean("right-sleeve-enabled",
                "Right sleeve enabled",
                "Right sleeve enabled",
                new String[]{"--right-sleeve-enabled"},
                true
        );
        public static final BooleanProperty LEFT_PANTS_LEG_ENABLED = BUILDER.ofBoolean("left-pants-leg-enabled",
                "Left pants leg enabled",
                "Left pants leg enabled",
                new String[]{"--left-pants-leg-enabled"},
                true
        );
        public static final BooleanProperty RIGHT_PANTS_LEG_ENABLED = BUILDER.ofBoolean("right-pants-leg-enabled",
                "Right pants leg enabled",
                "Right pants leg enabled",
                new String[]{"--right-pants-leg-enabled"},
                true
        );
        public static final BooleanProperty HAT_ENABLED = BUILDER.ofBoolean("hat-enabled",
                "Hat enabled",
                "Hat enabled",
                new String[]{"--hat-enabled"},
                true
        );
        public static final ComboProperty HAND_PREFERENCE = BUILDER.ofEnum("hand-preference",
                "Hand preference",
                "Hand preference",
                new String[]{"--hand-preference"},
                HandPreference.values(),
                HandPreference.RIGHT_HAND
        );
        public static final BooleanProperty TEXT_FILTERING_ENABLED = BUILDER.ofBoolean("text-filtering-enabled",
                "Text filtering enabled",
                "Text filtering enabled",
                new String[]{"--text-filtering-enabled"},
                true
        );
        public static final BooleanProperty ALLOWS_LISTING = BUILDER.ofBoolean("allows-listing",
                "Allows listing",
                "Allows listing",
                new String[]{"--allows-listing"},
                true
        );
    }
}