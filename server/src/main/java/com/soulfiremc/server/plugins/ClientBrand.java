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
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableStringProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.pf4j.Extension;

@Extension
public class ClientBrand extends InternalPlugin {
  public ClientBrand() {
    super(new PluginInfo(
      "client-brand",
      "1.0.0",
      "Sends the client brand to the server",
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

      if (!settingsSource.get(ClientBrandSettings.ENABLED)) {
        return;
      }

      var buf = Unpooled.buffer();
      MinecraftTypes.writeString(buf, settingsSource.get(ClientBrandSettings.CLIENT_BRAND));

      connection
        .session()
        .send(new ServerboundCustomPayloadPacket(SFProtocolConstants.BRAND_PAYLOAD_KEY, ByteBufUtil.getBytes(buf)));
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(ClientBrandSettings.class, "Client Brand", this, "fingerprint", ClientBrandSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ClientBrandSettings implements SettingsObject {
    private static final String NAMESPACE = "client-brand";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Send client brand")
        .description("Send client brand to the server")
        .defaultValue(true)
        .build();
    public static final StringProperty CLIENT_BRAND =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("client-brand")
        .uiName("Client brand")
        .description("The client brand to send to the server")
        .defaultValue("vanilla")
        .build();
  }
}
