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

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.bot.SFPacketSendingEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.Property;
import io.netty.buffer.Unpooled;
import java.util.List;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ModLoaderSupport implements InternalPlugin {
  private static final ResourceKey FML_HS_KEY = ResourceKey.fromString("fml:hs");
  private static final ResourceKey FML_FML_KEY = ResourceKey.fromString("fml:fml");
  private static final ResourceKey FML_MP_KEY = ResourceKey.fromString("fml:mp");
  private static final ResourceKey FML_FORGE_KEY = ResourceKey.fromString("fml:forge");
  private static final ResourceKey FML2_LOGIN_WRAPPER_KEY =
    ResourceKey.fromString("fml:loginwrapper");
  private static final ResourceKey FML2_HANDSHAKE_KEY = ResourceKey.fromString("fml:handshake");
  private static final char HOSTNAME_SEPARATOR = '\0';

  @EventHandler
  public static void onSettingsRegistryInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ModLoaderSettings.class, "Mod Loader Support");
  }

  private static String createFMLAddress(String initialHostname) {
    return initialHostname + HOSTNAME_SEPARATOR + "FML" + HOSTNAME_SEPARATOR;
  }

  private static String createFML2Address(String initialHostname) {
    return initialHostname + HOSTNAME_SEPARATOR + "FML2" + HOSTNAME_SEPARATOR;
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(ModLoaderSupport.class);
    PluginHelper.registerBotEventConsumer(SFPacketSendingEvent.class, this::onPacket);
    PluginHelper.registerBotEventConsumer(SFPacketReceiveEvent.class, this::onPacketReceive);
  }

  public void onPacket(SFPacketSendingEvent event) {
    if (!(event.packet() instanceof ClientIntentionPacket handshake)) {
      return;
    }

    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    var hostname = handshake.getHostname();

    switch (settingsHolder.get(
      ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)) {
      case FML -> event.packet(handshake.withHostname(createFMLAddress(hostname)));
      case FML2 -> event.packet(handshake.withHostname(createFML2Address(hostname)));
    }
  }

  public void onPacketReceive(SFPacketReceiveEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();

    if (event.packet() instanceof ClientboundCustomPayloadPacket pluginMessage) {
      var channelKey = ResourceKey.fromString(pluginMessage.getChannel());
      if (settingsHolder.get(ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)
        == ModLoaderSettings.ModLoaderMode.FML) {
        handleFMLPluginMessage(event.connection(), channelKey, pluginMessage.getData());
      }
    } else if (event.packet() instanceof ClientboundCustomQueryPacket loginPluginMessage) {
      var channelKey = ResourceKey.fromString(loginPluginMessage.getChannel());
      if (settingsHolder.get(ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)
        == ModLoaderSettings.ModLoaderMode.FML2) {
        handleFML2PluginMessage(event.connection(), channelKey, loginPluginMessage.getData());
      }
    }
  }

  private void handleFMLPluginMessage(
    BotConnection botConnection, ResourceKey channelKey, byte[] data) {
    if (!channelKey.equals(FML_HS_KEY)) {
      return;
    }

    var buffer = Unpooled.wrappedBuffer(data);
    var discriminator = buffer.readByte();
    switch (discriminator) {
      case 0 -> { // ServerHello
        var fmlProtocolVersion = buffer.readByte();
        var helper = botConnection.session().getCodecHelper();
        if (fmlProtocolVersion > 1) {
          var dimension = helper.readVarInt(buffer);
          log.debug("FML dimension override: {}", dimension);
        }

        botConnection
          .botControl()
          .registerPluginChannels(
            FML_HS_KEY, FML_FML_KEY, FML_MP_KEY, FML_FML_KEY, FML_FORGE_KEY);
        sendFMLClientHello(botConnection, fmlProtocolVersion);
        sendFMLModList(botConnection, List.of());
      }
      case 2 -> // ModList
        // WAITINGSERVERDATA
        sendFMLHandshakeAck(botConnection, (byte) 2);
      case 3 -> { // RegistryData
        var hasMore = buffer.readBoolean();
        if (!hasMore) {
          // WAITINGSERVERCOMPLETE
          sendFMLHandshakeAck(botConnection, (byte) 3);
        }
      }
      case -1 -> { // HandshakeAck
        var phase = buffer.readByte();
        switch (phase) {
          case 2 -> // WAITINGCACK
            // PENDINGCOMPLETE
            sendFMLHandshakeAck(botConnection, (byte) 4);
          case 3 -> // COMPLETE
            // COMPLETE
            sendFMLHandshakeAck(botConnection, (byte) 5);
        }
      }
      case -2 -> // HandshakeReset
        log.debug("FML handshake reset");
    }
  }

  private void sendFMLClientHello(BotConnection botConnection, byte fmlProtocolVersion) {
    var buffer = Unpooled.buffer();
    buffer.writeByte(1);
    buffer.writeByte(fmlProtocolVersion);

    botConnection.botControl().sendPluginMessage(FML_HS_KEY, buffer);
  }

  private void sendFMLModList(BotConnection botConnection, List<Mod> mods) {
    var helper = botConnection.session().getCodecHelper();
    var buffer = Unpooled.buffer();
    buffer.writeByte(2);
    helper.writeVarInt(buffer, mods.size());
    for (var mod : mods) {
      helper.writeString(buffer, mod.modId);
      helper.writeString(buffer, mod.version);
    }

    botConnection.botControl().sendPluginMessage(FML_HS_KEY, buffer);
  }

  private void sendFMLHandshakeAck(BotConnection botConnection, byte phase) {
    var buffer = Unpooled.buffer();
    buffer.writeByte(-1);
    buffer.writeByte(phase);

    botConnection.botControl().sendPluginMessage(FML_HS_KEY, buffer);
  }

  private void handleFML2PluginMessage(
    BotConnection botConnection, ResourceKey channelKey, byte[] data) {
    if (!channelKey.equals(FML2_LOGIN_WRAPPER_KEY)) {
      return;
    }

    var helper = botConnection.session().getCodecHelper();
    var buffer = Unpooled.wrappedBuffer(data);

    var innerChannel = helper.readString(buffer);
    var innerChannelKey = ResourceKey.fromString(innerChannel);
    if (!innerChannelKey.equals(FML2_HANDSHAKE_KEY)) {
      return;
    }

    var length = helper.readVarInt(buffer);
    var innerBuffer = buffer.readBytes(length);
    var packetId = helper.readVarInt(innerBuffer);
    var packetContentBuffer = innerBuffer.readBytes(innerBuffer.readableBytes());
    switch (packetId) {
      case 1 -> {
        var packetContentBytes = new byte[packetContentBuffer.readableBytes()];
        packetContentBuffer.readBytes(packetContentBytes);
        sendFML2HandshakeResponse(botConnection, 2, packetContentBytes);
      }
      case 3, 4 -> sendFML2HandshakeResponse(botConnection, 99, new byte[0]);
    }
  }

  private void sendFML2HandshakeResponse(
    BotConnection botConnection, int packetId, byte[] packetContent) {
    var helper = botConnection.session().getCodecHelper();
    var innerBuffer = Unpooled.buffer();
    helper.writeVarInt(innerBuffer, packetId);
    innerBuffer.writeBytes(packetContent);

    var buffer = Unpooled.buffer();
    helper.writeString(buffer, FML2_HANDSHAKE_KEY.toString());
    helper.writeVarInt(buffer, innerBuffer.readableBytes());
    buffer.writeBytes(innerBuffer);

    botConnection.botControl().sendPluginMessage(FML2_LOGIN_WRAPPER_KEY, buffer);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ModLoaderSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("mod-loader");
    public static final ComboProperty FORGE_MODE =
      BUILDER.ofEnum(
        "mod-loader-mode",
        "Mod Loader mode",
        new String[] {"--mod-loader-mode"},
        "What mod loader to use",
        ModLoaderMode.values(),
        ModLoaderMode.NONE);

    @RequiredArgsConstructor
    enum ModLoaderMode {
      NONE("None"),
      FML("FML (Forge 1.7-1.12)"),
      FML2("FML2 (Forge 1.13+)");

      private final String displayName;

      @Override
      public String toString() {
        return displayName;
      }
    }
  }

  private record Mod(String modId, String version) {}
}
