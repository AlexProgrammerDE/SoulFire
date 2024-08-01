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

import com.google.common.collect.ImmutableList;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.bot.SFPacketSendingEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.IdentifiedKey;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.server.util.UUIDHelper;
import com.soulfiremc.server.util.VelocityConstants;
import com.soulfiremc.util.GsonInstance;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ForwardingBypass implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "forwarding-bypass",
    "1.0.0",
    "Allows bypassing proxy forwarding",
    "AlexProgrammerDE",
    "GPL-3.0"
  );
  private static final char LEGACY_FORWARDING_SEPARATOR = '\0';

  public static void writePlayerKey(
    ByteBuf buf, MinecraftCodecHelper codecHelper, IdentifiedKey playerKey) {
    buf.writeLong(playerKey.expiryTemporal().toEpochMilli());
    codecHelper.writeByteArray(buf, playerKey.getSignedPublicKey().getEncoded());
    codecHelper.writeByteArray(buf, playerKey.getSignature());
  }

  private static int findForwardingVersion(int requested, BotConnection player) {
    // Ensure we are in range
    requested = Math.min(requested, VelocityConstants.MODERN_FORWARDING_MAX_VERSION);
    if (requested > VelocityConstants.MODERN_FORWARDING_DEFAULT) {
      if (player.protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_19_3)) {
        //noinspection NonStrictComparisonCanBeEquality
        return requested >= VelocityConstants.MODERN_LAZY_SESSION
          ? VelocityConstants.MODERN_LAZY_SESSION
          : VelocityConstants.MODERN_FORWARDING_DEFAULT;
      }
      if (player.identifiedKey() != null) {
        // No enhanced switch on java 11
        return switch (player.identifiedKey().getKeyRevision()) {
          case GENERIC_V1 -> VelocityConstants.MODERN_FORWARDING_WITH_KEY;
          case LINKED_V2 ->
            // Since V2 is not backwards compatible, we have to throw the key if v2 and requested
            // is v1
            requested >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
              ? VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
              : VelocityConstants.MODERN_FORWARDING_DEFAULT;
        };
      } else {
        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
      }
    }

    return VelocityConstants.MODERN_FORWARDING_DEFAULT;
  }

  private static ByteBuf createForwardingData(
    String hmacSecret, String address, BotConnection player, int requestedVersion) {
    var forwarded = Unpooled.buffer(2048);
    try {
      var actualVersion = findForwardingVersion(requestedVersion, player);

      var codecHelper = player.session().getCodecHelper();
      codecHelper.writeVarInt(forwarded, actualVersion);
      codecHelper.writeString(forwarded, address);
      codecHelper.writeUUID(forwarded, player.accountProfileId());
      codecHelper.writeString(forwarded, player.accountName());

      // This serves as additional redundancy. The key normally is stored in the
      // login start to the server, but some setups require this.
      if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY
        && actualVersion < VelocityConstants.MODERN_LAZY_SESSION) {
        var key = player.identifiedKey();
        assert key != null;
        writePlayerKey(forwarded, codecHelper, key);

        // Provide the signer UUID since the UUID may differ from the
        // assigned UUID. Doing that breaks the signatures anyway but the server
        // should be able to verify the key independently.
        if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2) {
          if (key.getSignatureHolder() != null) {
            forwarded.writeBoolean(true);
            codecHelper.writeUUID(forwarded, key.getSignatureHolder());
          } else {
            // Should only not be provided if the player was connected
            // as offline-mode and the signer UUID was not backfilled
            forwarded.writeBoolean(false);
          }
        }
      }

      var key = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      mac.update(forwarded.array(), forwarded.arrayOffset(), forwarded.readableBytes());
      var sig = mac.doFinal();

      return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded);
    } catch (InvalidKeyException e) {
      forwarded.release();
      throw new RuntimeException("Unable to authenticate data", e);
    } catch (NoSuchAlgorithmException e) {
      // Should never happen
      forwarded.release();
      throw new AssertionError(e);
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ForwardingBypassSettings.class, "Forwarding Bypass", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(ForwardingBypass.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, SFPacketSendingEvent.class, this::onPacket);
    PluginHelper.registerBotEventConsumer(soulFireServer, SFPacketReceiveEvent.class, this::onPacketReceive);
  }

  public void onPacket(SFPacketSendingEvent event) {
    if (!(event.packet() instanceof ClientIntentionPacket handshake)) {
      return;
    }

    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    var hostname = handshake.getHostname();
    var uuid = connection.accountProfileId();

    switch (settingsHolder.get(
      ForwardingBypassSettings.FORWARDING_MODE, ForwardingBypassSettings.ForwardingMode.class)) {
      case LEGACY -> event.packet(
        handshake.withHostname(
          createLegacyForwardingAddress(uuid, getForwardedIp(), hostname)));
      case BUNGEE_GUARD -> event.packet(
        handshake.withHostname(
          createBungeeGuardForwardingAddress(
            uuid,
            getForwardedIp(),
            hostname,
            settingsHolder.get(ForwardingBypassSettings.SECRET))));
    }
  }

  public void onPacketReceive(SFPacketReceiveEvent event) {
    if (!(event.packet() instanceof ClientboundCustomQueryPacket loginPluginMessage)) {
      return;
    }

    if (!loginPluginMessage.getChannel().equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
      return;
    }

    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (settingsHolder.get(
      ForwardingBypassSettings.FORWARDING_MODE, ForwardingBypassSettings.ForwardingMode.class)
      != ForwardingBypassSettings.ForwardingMode.MODERN) {
      log.warn("Received modern forwarding request packet, but forwarding mode is not modern!");
      return;
    }

    var requestedForwardingVersion = VelocityConstants.MODERN_FORWARDING_DEFAULT;
    {
      var buf = Unpooled.wrappedBuffer(loginPluginMessage.getData());
      if (buf.readableBytes() == 1) {
        requestedForwardingVersion = buf.readByte();
      }
    }

    var forwardingData =
      createForwardingData(
        settingsHolder.get(ForwardingBypassSettings.SECRET),
        getForwardedIp(),
        connection,
        requestedForwardingVersion);

    var bytes = new byte[forwardingData.readableBytes()];
    forwardingData.readBytes(bytes);
    var response = new ServerboundCustomQueryAnswerPacket(loginPluginMessage.getMessageId(), bytes);
    connection.session().send(response);
  }

  private String getForwardedIp() {
    return "127.0.0.1";
  }

  /*
   * This is a modified version of the code from <a href="https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/VelocityServerConnection.java#L171">Velocity</a>.
   */
  private String createLegacyForwardingAddress(
    UUID botUniqueId,
    String selfIp,
    String initialHostname,
    UnaryOperator<List<GameProfile.Property>> propertiesTransform) {
    // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
    // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
    // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
    var data =
      new StringBuilder(initialHostname)
        .append(LEGACY_FORWARDING_SEPARATOR)
        .append(selfIp)
        .append(LEGACY_FORWARDING_SEPARATOR)
        .append(UUIDHelper.convertToNoDashes(botUniqueId))
        .append(LEGACY_FORWARDING_SEPARATOR);
    GsonInstance.GSON.toJson(propertiesTransform.apply(List.of()), data);
    return data.toString();
  }

  private String createLegacyForwardingAddress(
    UUID botUniqueId, String selfIp, String initialHostname) {
    return createLegacyForwardingAddress(
      botUniqueId, selfIp, initialHostname, UnaryOperator.identity());
  }

  private String createBungeeGuardForwardingAddress(
    UUID botUniqueId, String selfIp, String initialHostname, String forwardingSecret) {
    // Append forwarding secret as a BungeeGuard token.
    var property = new GameProfile.Property("bungeeguard-token", forwardingSecret, "");
    return createLegacyForwardingAddress(
      botUniqueId,
      selfIp,
      initialHostname,
      properties ->
        ImmutableList.<GameProfile.Property>builder().addAll(properties).add(property).build());
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ForwardingBypassSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("forwarding-bypass");
    public static final ComboProperty FORWARDING_MODE =
      BUILDER.ofEnum(
        "forwarding-mode",
        "Forwarding mode",
        new String[] {"--forwarding-mode"},
        "What type of forwarding to use",
        ForwardingMode.values(),
        ForwardingMode.NONE);
    public static final StringProperty SECRET =
      BUILDER.ofStringSecret(
        "secret",
        "Secret",
        new String[] {"--secret"},
        "Secret key used for forwarding. (Not needed for legacy mode)",
        "forwarding secret");

    @RequiredArgsConstructor
    enum ForwardingMode {
      NONE("None"),
      LEGACY("Legacy"),
      BUNGEE_GUARD("BungeeGuard"),
      MODERN("Modern");

      private final String displayName;

      @Override
      public String toString() {
        return displayName;
      }
    }
  }
}
