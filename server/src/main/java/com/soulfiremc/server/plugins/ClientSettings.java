/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettings implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "client-settings",
    "1.0.0",
    "Sends client settings to the server",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  public static void onPacket(SFPacketReceiveEvent event) {
    if (event.packet() instanceof ClientboundGameProfilePacket) {
      var connection = event.connection();
      var settingsHolder = connection.settingsHolder();
      if (!settingsHolder.get(ClientSettingsSettings.ENABLED)) {
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

      event
        .connection()
        .session()
        .send(
          new ServerboundClientInformationPacket(
            settingsHolder.get(ClientSettingsSettings.CLIENT_LOCALE),
            settingsHolder.get(ClientSettingsSettings.RENDER_DISTANCE),
            settingsHolder.get(ClientSettingsSettings.CHAT_VISIBILITY, ChatVisibility.class),
            settingsHolder.get(ClientSettingsSettings.USE_CHAT_COLORS),
            skinParts,
            settingsHolder.get(ClientSettingsSettings.HAND_PREFERENCE, HandPreference.class),
            settingsHolder.get(ClientSettingsSettings.TEXT_FILTERING_ENABLED),
            settingsHolder.get(ClientSettingsSettings.ALLOWS_LISTING)));
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ClientSettingsSettings.class, "Client Settings", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(ClientSettings.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, SFPacketReceiveEvent.class, ClientSettings::onPacket);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class ClientSettingsSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("client-settings");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Send client settings",
        new String[] {"--send-client-settings"},
        "Send client settings to the server when joining",
        true);
    public static final StringProperty CLIENT_LOCALE =
      BUILDER.ofString(
        "client-locale",
        "Client locale",
        new String[] {"--client-locale"},
        "The locale the client uses for translations",
        "en_gb");
    public static final IntProperty RENDER_DISTANCE =
      BUILDER.ofInt(
        "render-distance",
        "Render distance",
        new String[] {"--render-distance"},
        "How far the client renders chunks. (Use this to load more or less chunks from the server)",
        8,
        2,
        32,
        1);
    public static final ComboProperty CHAT_VISIBILITY =
      BUILDER.ofEnumMapped(
        "chat-visibility",
        "Chat visibility",
        new String[] {"--chat-visibility"},
        "What type of chat messages the client will receive",
        ChatVisibility.values(),
        ChatVisibility.FULL,
        ComboProperty::capitalizeEnum);
    public static final BooleanProperty USE_CHAT_COLORS =
      BUILDER.ofBoolean(
        "use-chat-colors",
        "Use chat colors",
        new String[] {"--use-chat-colors"},
        "Whether the client will use chat colors",
        true);
    public static final BooleanProperty CAPE_ENABLED =
      BUILDER.ofBoolean(
        "cape-enabled",
        "Cape enabled",
        new String[] {"--cape-enabled"},
        "Whether to display the bots cape if it has one",
        true);
    public static final BooleanProperty JACKET_ENABLED =
      BUILDER.ofBoolean(
        "jacket-enabled",
        "Jacket enabled",
        new String[] {"--jacket-enabled"},
        "Whether to render the jacket overlay skin layer",
        true);
    public static final BooleanProperty LEFT_SLEEVE_ENABLED =
      BUILDER.ofBoolean(
        "left-sleeve-enabled",
        "Left sleeve enabled",
        new String[] {"--left-sleeve-enabled"},
        "Whether to render the left overlay skin layer",
        true);
    public static final BooleanProperty RIGHT_SLEEVE_ENABLED =
      BUILDER.ofBoolean(
        "right-sleeve-enabled",
        "Right sleeve enabled",
        new String[] {"--right-sleeve-enabled"},
        "Whether to render the right overlay skin layer",
        true);
    public static final BooleanProperty LEFT_PANTS_LEG_ENABLED =
      BUILDER.ofBoolean(
        "left-pants-leg-enabled",
        "Left pants leg enabled",
        new String[] {"--left-pants-leg-enabled"},
        "Whether to render the left pants leg overlay skin layer",
        true);
    public static final BooleanProperty RIGHT_PANTS_LEG_ENABLED =
      BUILDER.ofBoolean(
        "right-pants-leg-enabled",
        "Right pants leg enabled",
        new String[] {"--right-pants-leg-enabled"},
        "Whether to render the right pants leg overlay skin layer",
        true);
    public static final BooleanProperty HAT_ENABLED =
      BUILDER.ofBoolean(
        "hat-enabled",
        "Hat enabled",
        new String[] {"--hat-enabled"},
        "Whether to render the hat overlay skin layer",
        true);
    public static final ComboProperty HAND_PREFERENCE =
      BUILDER.ofEnumMapped(
        "hand-preference",
        "Hand preference",
        new String[] {"--hand-preference"},
        "What hand the client prefers to use for items",
        HandPreference.values(),
        HandPreference.RIGHT_HAND,
        ComboProperty::capitalizeEnum);
    public static final BooleanProperty TEXT_FILTERING_ENABLED =
      BUILDER.ofBoolean(
        "text-filtering-enabled",
        "Text filtering enabled",
        new String[] {"--text-filtering-enabled"},
        "Whether to filter chat messages from the server",
        true);
    public static final BooleanProperty ALLOWS_LISTING =
      BUILDER.ofBoolean(
        "allows-listing",
        "Allows listing",
        new String[] {"--allows-listing"},
        "Whether the client wants their username to be shown in the server list",
        true);
  }
}
