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

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.pf4j.Extension;

import java.util.ArrayList;

@Extension
public class ClientSettings extends InternalPlugin {
  public ClientSettings() {
    super(new PluginInfo(
      "client-settings",
      "1.0.0",
      "Sends client settings to the server",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onPacket(SFPacketReceiveEvent event) {
    if (event.packet() instanceof ClientboundLoginFinishedPacket) {
      var connection = event.connection();
      var settingsSource = connection.settingsSource();
      if (!settingsSource.get(ClientSettingsSettings.ENABLED)) {
        return;
      }

      var skinParts = new ArrayList<SkinPart>();
      for (var part : SkinPart.values()) {
        if (switch (part) {
          case CAPE -> settingsSource.get(ClientSettingsSettings.CAPE_ENABLED);
          case JACKET -> settingsSource.get(ClientSettingsSettings.JACKET_ENABLED);
          case LEFT_SLEEVE -> settingsSource.get(ClientSettingsSettings.LEFT_SLEEVE_ENABLED);
          case RIGHT_SLEEVE -> settingsSource.get(ClientSettingsSettings.RIGHT_SLEEVE_ENABLED);
          case LEFT_PANTS_LEG -> settingsSource.get(ClientSettingsSettings.LEFT_PANTS_LEG_ENABLED);
          case RIGHT_PANTS_LEG -> settingsSource.get(ClientSettingsSettings.RIGHT_PANTS_LEG_ENABLED);
          case HAT -> settingsSource.get(ClientSettingsSettings.HAT_ENABLED);
        }) {
          skinParts.add(part);
        }
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
            settingsSource.get(ClientSettingsSettings.ALLOWS_LISTING),
            settingsSource.get(ClientSettingsSettings.PARTICLE_STATUS, ParticleStatus.class)));
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(ClientSettingsSettings.class, "Client Settings", this, "settings-2", ClientSettingsSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class ClientSettingsSettings implements SettingsObject {
    private static final String NAMESPACE = "client-settings";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Send client settings")
        .description("Send client settings to the server when joining")
        .defaultValue(true)
        .build();
    public static final StringProperty CLIENT_LOCALE =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("client-locale")
        .uiName("Client locale")
        .description("The locale the client uses for translations")
        .defaultValue("en_gb")
        .build();
    public static final IntProperty RENDER_DISTANCE =
      ImmutableIntProperty.builder()
        .namespace(NAMESPACE)
        .key("render-distance")
        .uiName("Render distance")
        .description("How far the client renders chunks. (Use this to load more or less chunks from the server)")
        .defaultValue(8)
        .minValue(2)
        .maxValue(32)
        .build();
    public static final ComboProperty CHAT_VISIBILITY =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("chat-visibility")
        .uiName("Chat visibility")
        .description("What type of chat messages the client will receive")
        .defaultValue(ChatVisibility.FULL.name())
        .addOptions(ComboProperty.optionsFromEnum(ChatVisibility.values(), ComboProperty::capitalizeEnum))
        .build();
    public static final BooleanProperty USE_CHAT_COLORS =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("use-chat-colors")
        .uiName("Use chat colors")
        .description("Whether the client will use chat colors")
        .defaultValue(true)
        .build();
    public static final BooleanProperty CAPE_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("cape-enabled")
        .uiName("Cape enabled")
        .description("Whether to display the bots cape if it has one")
        .defaultValue(true)
        .build();
    public static final BooleanProperty JACKET_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("jacket-enabled")
        .uiName("Jacket enabled")
        .description("Whether to render the jacket overlay skin layer")
        .defaultValue(true)
        .build();
    public static final BooleanProperty LEFT_SLEEVE_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("left-sleeve-enabled")
        .uiName("Left sleeve enabled")
        .description("Whether to render the left overlay skin layer")
        .defaultValue(true)
        .build();
    public static final BooleanProperty RIGHT_SLEEVE_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("right-sleeve-enabled")
        .uiName("Right sleeve enabled")
        .description("Whether to render the right overlay skin layer")
        .defaultValue(true)
        .build();
    public static final BooleanProperty LEFT_PANTS_LEG_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("left-pants-leg-enabled")
        .uiName("Left pants leg enabled")
        .description("Whether to render the left pants leg overlay skin layer")
        .defaultValue(true)
        .build();
    public static final BooleanProperty RIGHT_PANTS_LEG_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("right-pants-leg-enabled")
        .uiName("Right pants leg enabled")
        .description("Whether to render the right pants leg overlay skin layer")
        .defaultValue(true)
        .build();
    public static final BooleanProperty HAT_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("hat-enabled")
        .uiName("Hat enabled")
        .description("Whether to render the hat overlay skin layer")
        .defaultValue(true)
        .build();
    public static final ComboProperty HAND_PREFERENCE =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("hand-preference")
        .uiName("Hand preference")
        .description("What hand the client prefers to use for items")
        .defaultValue(HandPreference.RIGHT_HAND.name())
        .addOptions(ComboProperty.optionsFromEnum(HandPreference.values(), ComboProperty::capitalizeEnum))
        .build();
    public static final BooleanProperty TEXT_FILTERING_ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("text-filtering-enabled")
        .uiName("Text filtering enabled")
        .description("Whether to filter chat messages from the server")
        .defaultValue(true)
        .build();
    public static final BooleanProperty ALLOWS_LISTING =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("allows-listing")
        .uiName("Allows listing")
        .description("Whether the client wants their username to be shown in the server list")
        .defaultValue(true)
        .build();
    public static final ComboProperty PARTICLE_STATUS =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("particle-status")
        .uiName("Particle Status")
        .description("How many particles the client will render")
        .defaultValue(ParticleStatus.ALL.name())
        .addOptions(ComboProperty.optionsFromEnum(ParticleStatus.values(), ComboProperty::capitalizeEnum))
        .build();
  }
}
