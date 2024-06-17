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

import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;

public class ClientBrand implements InternalPlugin {
  public static void onPacket(SFPacketReceiveEvent event) {
    if (event.packet() instanceof ClientboundGameProfilePacket) {
      var connection = event.connection();
      var settingsHolder = connection.settingsHolder();

      if (!settingsHolder.get(ClientBrandSettings.ENABLED)) {
        return;
      }

      var buf = Unpooled.buffer();
      connection
        .session()
        .getCodecHelper()
        .writeString(buf, settingsHolder.get(ClientBrandSettings.CLIENT_BRAND));

      connection
        .session()
        .send(new ServerboundCustomPayloadPacket(SFProtocolConstants.BRAND_PAYLOAD_KEY, ByteBufUtil.getBytes(buf)));
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ClientBrandSettings.class, "Client Brand");
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(ClientBrand.class);
    PluginHelper.registerBotEventConsumer(SFPacketReceiveEvent.class, ClientBrand::onPacket);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ClientBrandSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("client-brand");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Send client brand",
        new String[] {"--send-client-brand"},
        "Send client brand to the server",
        true);
    public static final StringProperty CLIENT_BRAND =
      BUILDER.ofString(
        "client-brand",
        "Client brand",
        new String[] {"--client-brand"},
        "The client brand to send to the server",
        "vanilla");
  }
}
