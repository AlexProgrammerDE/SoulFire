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
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;

import javax.inject.Inject;
import java.util.ArrayList;

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
      var settingsSource = connection.settingsSource();
      if (!settingsSource.get(ClientSettingsSettings.ENABLED)) {
        return;
      }

      var skinParts = new ArrayList<SkinPart>();
      if (settingsSource.get(ClientSettingsSettings.CAPE_ENABLED)) {
        skinParts.add(SkinPart.CAPE);
      }
      if (settingsSource.get(ClientSettingsSettings.JACKET_ENABLED)) {
        skinParts.add(SkinPart.JACKET);
      }
      if (settingsSource.get(ClientSettingsSettings.LEFT_SLEEVE_ENABLED)) {
        skinParts.add(SkinPart.LEFT_SLEEVE);
      }
      if (settingsSource.get(ClientSettingsSettings.RIGHT_SLEEVE_ENABLED)) {
        skinParts.add(SkinPart.RIGHT_SLEEVE);
      }
      if (settingsSource.get(ClientSettingsSettings.LEFT_PANTS_LEG_ENABLED)) {
        skinParts.add(SkinPart.LEFT_PANTS_LEG);
      }
      if (settingsSource.get(ClientSettingsSettings.RIGHT_PANTS_LEG_ENABLED)) {
        skinParts.add(SkinPart.RIGHT_PANTS_LEG);
      }
      if (settingsSource.get(ClientSettingsSettings.HAT_ENABLED)) {
        skinParts.add(SkinPart.HAT);
      }

      event
        .connection()
        .session()
        .send(
          new ServerboundClientInformationPacket(
            settingsSource.get(ClientSettingsSettings.CLIENT_LOCALE),
            settingsSource.get(ClientSettingsSettings.RENDER_DISTANCE),
            settingsSource.get(ClientSettingsSettings.CHAT_VISIBILITY, ChatVisibility.class),
            settingsSource.get(ClientSettingsSettings.USE_CHAT_COLORS),
            skinParts,
            settingsSource.get(ClientSettingsSettings.HAND_PREFERENCE, HandPreference.class),
            settingsSource.get(ClientSettingsSettings.TEXT_FILTERING_ENABLED),
            settingsSource.get(ClientSettingsSettings.ALLOWS_LISTING)));
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ClientSettingsSettings.class, "Client Settings", PLUGIN_INFO, "settings-2");
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
        "Send client settings to the server when joining",
        true);
    public static final StringProperty CLIENT_LOCALE =
      BUILDER.ofString(
        "client-locale",
        "Client locale",
        "The locale the client uses for translations",
        "en_gb");
    public static final IntProperty RENDER_DISTANCE =
      BUILDER.ofInt(
        "render-distance",
        "Render distance",
        "How far the client renders chunks. (Use this to load more or less chunks from the server)",
        8,
        2,
        32,
        1);
    public static final ComboProperty CHAT_VISIBILITY =
      BUILDER.ofEnumMapped(
        "chat-visibility",
        "Chat visibility",
        "What type of chat messages the client will receive",
        ChatVisibility.values(),
        ChatVisibility.FULL,
        ComboProperty::capitalizeEnum);
    public static final BooleanProperty USE_CHAT_COLORS =
      BUILDER.ofBoolean(
        "use-chat-colors",
        "Use chat colors",
        "Whether the client will use chat colors",
        true);
    public static final BooleanProperty CAPE_ENABLED =
      BUILDER.ofBoolean(
        "cape-enabled",
        "Cape enabled",
        "Whether to display the bots cape if it has one",
        true);
    public static final BooleanProperty JACKET_ENABLED =
      BUILDER.ofBoolean(
        "jacket-enabled",
        "Jacket enabled",
        "Whether to render the jacket overlay skin layer",
        true);
    public static final BooleanProperty LEFT_SLEEVE_ENABLED =
      BUILDER.ofBoolean(
        "left-sleeve-enabled",
        "Left sleeve enabled",
        "Whether to render the left overlay skin layer",
        true);
    public static final BooleanProperty RIGHT_SLEEVE_ENABLED =
      BUILDER.ofBoolean(
        "right-sleeve-enabled",
        "Right sleeve enabled",
        "Whether to render the right overlay skin layer",
        true);
    public static final BooleanProperty LEFT_PANTS_LEG_ENABLED =
      BUILDER.ofBoolean(
        "left-pants-leg-enabled",
        "Left pants leg enabled",
        "Whether to render the left pants leg overlay skin layer",
        true);
    public static final BooleanProperty RIGHT_PANTS_LEG_ENABLED =
      BUILDER.ofBoolean(
        "right-pants-leg-enabled",
        "Right pants leg enabled",
        "Whether to render the right pants leg overlay skin layer",
        true);
    public static final BooleanProperty HAT_ENABLED =
      BUILDER.ofBoolean(
        "hat-enabled",
        "Hat enabled",
        "Whether to render the hat overlay skin layer",
        true);
    public static final ComboProperty HAND_PREFERENCE =
      BUILDER.ofEnumMapped(
        "hand-preference",
        "Hand preference",
        "What hand the client prefers to use for items",
        HandPreference.values(),
        HandPreference.RIGHT_HAND,
        ComboProperty::capitalizeEnum);
    public static final BooleanProperty TEXT_FILTERING_ENABLED =
      BUILDER.ofBoolean(
        "text-filtering-enabled",
        "Text filtering enabled",
        "Whether to filter chat messages from the server",
        true);
    public static final BooleanProperty ALLOWS_LISTING =
      BUILDER.ofBoolean(
        "allows-listing",
        "Allows listing",
        "Whether the client wants their username to be shown in the server list",
        true);
  }
}
