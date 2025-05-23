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
package com.soulfiremc.server.protocol;

import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.storage.ProtocolMetadataStorage;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;
import org.geysermc.mcprotocollib.network.compression.ZlibCompression;
import org.geysermc.mcprotocollib.network.crypt.AESEncryption;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.serverbound.ServerboundPingRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public final class SFBaseListener extends SessionAdapter {
  private final BotConnection botConnection;
  private final @NonNull HandshakeIntent targetState;

  @SneakyThrows
  @Override
  public void packetReceived(Session session, Packet packet) {
    var protocol = session.getPacketProtocol();
    switch (protocol.getInboundState()) {
      case LOGIN -> {
        switch (packet) {
          case ClientboundHelloPacket helloPacket -> {
            var viaUserConnection = session.getFlag(SFProtocolConstants.VIA_USER_CONNECTION);

            var needsAuth = helloPacket.isShouldAuthenticate();
            var isLegacy = SFVersionConstants.isLegacy(botConnection.protocolVersion());
            if (needsAuth && isLegacy) {
              needsAuth =
                Objects.requireNonNull(viaUserConnection.get(ProtocolMetadataStorage.class))
                  .authenticate;
            }

            SecretKey key;
            try {
              var gen = KeyGenerator.getInstance("AES");
              gen.init(128);
              key = gen.generateKey();
            } catch (NoSuchAlgorithmException e) {
              throw new IllegalStateException("Failed to generate shared key.", e);
            }

            log.debug("Needs auth: {}", needsAuth);
            if (needsAuth) {
              var canDoAuth = botConnection.minecraftAccount().isPremiumJava();
              log.debug("Can do auth: {}", canDoAuth);
              if (canDoAuth) {
                var serverId =
                  SessionService.getServerId(
                    helloPacket.getServerId(), helloPacket.getPublicKey(), key);
                botConnection.joinServerId(serverId);
              } else {
                log.info("Server sent a encryption request, but account is offline mode. Not authenticating with mojang.");
              }
            }

            var keyPacket = new ServerboundKeyPacket(helloPacket.getPublicKey(), key, helloPacket.getChallenge());

            var encryptionConfig = new EncryptionConfig(new AESEncryption(key));
            if (!isLegacy) {
              session.send(keyPacket, () -> session.setEncryption(encryptionConfig));
            } else {
              log.debug("Storing legacy secret key.");
              session.setFlag(SFProtocolConstants.VL_ENCRYPTION_CONFIG, encryptionConfig);
            }
          }
          case ClientboundLoginFinishedPacket ignored -> {
            session.switchInboundState(() -> protocol.setInboundState(ProtocolState.CONFIGURATION));
            session.send(new ServerboundLoginAcknowledgedPacket());
            session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.CONFIGURATION));
          }
          case ClientboundLoginDisconnectPacket loginDisconnectPacket -> session.disconnect(loginDisconnectPacket.getReason());
          case ClientboundLoginCompressionPacket loginCompressionPacket -> {
            if (loginCompressionPacket.getThreshold() >= 0) {
              session.setCompression(new CompressionConfig(loginCompressionPacket.getThreshold(), new ZlibCompression(), true));
            }
          }
          default -> {
          }
        }
      }
      case STATUS -> {
        switch (packet) {
          case ClientboundStatusResponsePacket ignored -> session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
          case ClientboundPongResponsePacket ignored -> session.disconnect(Component.translatable("multiplayer.status.finished"));
          default -> {
          }
        }
      }
      case GAME -> {
        switch (packet) {
          case ClientboundKeepAlivePacket keepAlivePacket -> session.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
          case ClientboundPingPacket pingPacket -> session.send(new ServerboundPongPacket(pingPacket.getId()));
          case ClientboundDisconnectPacket disconnectPacket -> session.disconnect(disconnectPacket.getReason());
          case ClientboundStartConfigurationPacket ignored -> {
            botConnection.clearClientLevel();
            session.switchInboundState(() -> protocol.setInboundState(ProtocolState.CONFIGURATION));
            session.send(new ServerboundConfigurationAcknowledgedPacket());
            session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.CONFIGURATION));
          }
          default -> {
          }
        }
      }
      case CONFIGURATION -> {
        switch (packet) {
          case ClientboundFinishConfigurationPacket ignored -> {
            session.switchInboundState(() -> protocol.setInboundState(ProtocolState.GAME));
            session.send(new ServerboundFinishConfigurationPacket());
            session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.GAME));
          }
          case ClientboundSelectKnownPacks selectKnownPacks -> session.send(new ServerboundSelectKnownPacks(BuiltInKnownPackRegistry.INSTANCE
            .getMatchingPacks(selectKnownPacks.getKnownPacks())));
          default -> {
          }
        }
      }
      case null, default -> {
      }
    }
  }

  @Override
  public void connected(ConnectedEvent event) {
    var resolvedAddress = botConnection.resolvedAddress().resolvedAddress();
    var session = event.getSession();
    var protocol = session.getPacketProtocol();
    var intention = new ClientIntentionPacket(
      protocol.getCodec().getProtocolVersion(),
      resolvedAddress.getHostName(),
      resolvedAddress.getPort(),
      targetState
    );

    var protocolState = switch (this.targetState) {
      case STATUS -> ProtocolState.STATUS;
      case LOGIN, TRANSFER -> ProtocolState.LOGIN;
    };
    session.switchInboundState(() -> protocol.setInboundState(protocolState));
    session.send(intention);
    session.switchOutboundState(() -> protocol.setOutboundState(protocolState));
    SFHelpers.mustSupply(() -> switch (this.targetState) {
      case LOGIN, TRANSFER -> () -> session.send(new ServerboundHelloPacket(botConnection.accountName(), botConnection.accountProfileId()));
      case STATUS -> () -> session.send(new ServerboundStatusRequestPacket());
    });
  }
}
