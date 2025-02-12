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
import com.soulfiremc.server.api.event.bot.SFPacketSendingEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableComboProperty;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.pf4j.Extension;

import java.util.List;

@Slf4j
@Extension(ordinal = 1)
public class ModLoaderSupport extends InternalPlugin {
  private static final Key FML_HS_KEY = Key.key("fml:hs");
  private static final Key FML_FML_KEY = Key.key("fml:fml");
  private static final Key FML_MP_KEY = Key.key("fml:mp");
  private static final Key FML_FORGE_KEY = Key.key("fml:forge");
  private static final Key FML2_LOGIN_WRAPPER_KEY =
    Key.key("fml:loginwrapper");
  private static final Key FML2_HANDSHAKE_KEY = Key.key("fml:handshake");
  private static final char HOSTNAME_SEPARATOR = '\0';

  public ModLoaderSupport() {
    super(new PluginInfo(
      "mod-loader-support",
      "1.0.0",
      "Supports mod loaders like Forge",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  private static String createFMLAddress(String initialHostname) {
    return initialHostname + HOSTNAME_SEPARATOR + "FML" + HOSTNAME_SEPARATOR;
  }

  private static String createFML2Address(String initialHostname) {
    return initialHostname + HOSTNAME_SEPARATOR + "FML2" + HOSTNAME_SEPARATOR;
  }

  @Override
  public boolean isAvailable() {
    return Boolean.getBoolean("sf.mod_support");
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(ModLoaderSettings.class, "Mod Loader Support", this, "package", ModLoaderSettings.ENABLED);
  }

  @EventHandler
  public void onPacket(SFPacketSendingEvent event) {
    if (!(event.packet() instanceof ClientIntentionPacket handshake)) {
      return;
    }

    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    var hostname = handshake.getHostname();

    switch (settingsSource.get(
      ModLoaderSettings.MOD_LOADER_MODE, ModLoaderSettings.ModLoaderMode.class)) {
      case FML -> event.packet(handshake.withHostname(createFMLAddress(hostname)));
      case FML2 -> event.packet(handshake.withHostname(createFML2Address(hostname)));
    }
  }

  @EventHandler
  public void onPacketReceive(SFPacketReceiveEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();

    if (event.packet() instanceof ClientboundCustomPayloadPacket pluginMessage) {
      var channelKey = pluginMessage.getChannel();
      if (settingsSource.get(ModLoaderSettings.MOD_LOADER_MODE, ModLoaderSettings.ModLoaderMode.class)
        == ModLoaderSettings.ModLoaderMode.FML) {
        handleFMLPluginMessage(event.connection(), channelKey, pluginMessage.getData());
      }
    } else if (event.packet() instanceof ClientboundCustomQueryPacket loginPluginMessage) {
      var channelKey = loginPluginMessage.getChannel();
      if (settingsSource.get(ModLoaderSettings.MOD_LOADER_MODE, ModLoaderSettings.ModLoaderMode.class)
        == ModLoaderSettings.ModLoaderMode.FML2) {
        handleFML2PluginMessage(event.connection(), channelKey, loginPluginMessage.getData());
      }
    }
  }

  private void handleFMLPluginMessage(
    BotConnection botConnection, Key channelKey, byte[] data) {
    if (!channelKey.equals(FML_HS_KEY)) {
      return;
    }

    var buffer = Unpooled.wrappedBuffer(data);
    var discriminator = buffer.readByte();
    switch (discriminator) {
      case 0 -> { // ServerHello
        var fmlProtocolVersion = buffer.readByte();
        if (fmlProtocolVersion > 1) {
          var dimension = MinecraftTypes.readVarInt(buffer);
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
    var buffer = Unpooled.buffer();
    buffer.writeByte(2);
    MinecraftTypes.writeVarInt(buffer, mods.size());
    for (var mod : mods) {
      MinecraftTypes.writeString(buffer, mod.modId);
      MinecraftTypes.writeString(buffer, mod.version);
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
    BotConnection botConnection, Key channelKey, byte[] data) {
    if (!channelKey.equals(FML2_LOGIN_WRAPPER_KEY)) {
      return;
    }

    var buffer = Unpooled.wrappedBuffer(data);

    var innerChannelKey = MinecraftTypes.readResourceLocation(buffer);
    if (!innerChannelKey.equals(FML2_HANDSHAKE_KEY)) {
      return;
    }

    var length = MinecraftTypes.readVarInt(buffer);
    var innerBuffer = buffer.readBytes(length);
    var packetId = MinecraftTypes.readVarInt(innerBuffer);
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
    var innerBuffer = Unpooled.buffer();
    MinecraftTypes.writeVarInt(innerBuffer, packetId);
    innerBuffer.writeBytes(packetContent);

    var buffer = Unpooled.buffer();
    MinecraftTypes.writeString(buffer, FML2_HANDSHAKE_KEY.toString());
    MinecraftTypes.writeVarInt(buffer, innerBuffer.readableBytes());
    buffer.writeBytes(innerBuffer);

    botConnection.botControl().sendPluginMessage(FML2_LOGIN_WRAPPER_KEY, buffer);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ModLoaderSettings implements SettingsObject {
    private static final String NAMESPACE = "mod-loader";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable mod loader support")
        .description("Enable the mod loader support")
        .defaultValue(false)
        .build();
    public static final ComboProperty MOD_LOADER_MODE =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("mod-loader-mode")
        .uiName("Mod Loader mode")
        .description("What mod loader to use")
        .defaultValue(ModLoaderMode.FML2.name())
        .addOptions(ComboProperty.optionsFromEnum(ModLoaderMode.values(), ModLoaderMode::toString))
        .build();

    @RequiredArgsConstructor
    enum ModLoaderMode {
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
