/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoHandler;
import com.github.steveice10.mc.protocol.data.status.handler.ServerPingTimeHandler;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaTcpClientSession;
import org.slf4j.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
public class SWBaseListener extends SessionAdapter {
    private final Logger logger;
    private final @NonNull ProtocolState targetState;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(session instanceof ViaTcpClientSession viaSession)) {
            return;
        }

        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            if (packet instanceof ClientboundHelloPacket helloPacket) {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

                SecretKey key;
                try {
                    KeyGenerator gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    key = gen.generateKey();
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Failed to generate shared key.", e);
                }

                session.setFlag(SWProtocolConstants.ENCRYPTION_SECRET_KEY, key);
                session.send(new ServerboundKeyPacket(helloPacket.getPublicKey(), key, helloPacket.getChallenge()));

                if (profile == null || accessToken == null) {
                    logger.debug("Skipping hello packet due to missing profile or access token.");
                } else {
                    SessionService sessionService = session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
                    String serverId = sessionService.getServerId(helloPacket.getServerId(), helloPacket.getPublicKey(), key);
                    try {
                        sessionService.joinServer(profile, accessToken, serverId);
                    } catch (ServiceUnavailableException e) {
                        session.disconnect("Login failed: Authentication service unavailable.", e);
                        return;
                    } catch (InvalidCredentialsException e) {
                        session.disconnect("Login failed: Invalid login session.", e);
                        return;
                    } catch (RequestException e) {
                        session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
                        return;
                    }

                    viaSession.enableEncryption(key);
                }
            } else if (packet instanceof ClientboundGameProfilePacket) {
                protocol.setState(ProtocolState.GAME);
            } else if (packet instanceof ClientboundLoginDisconnectPacket) {
                session.disconnect(((ClientboundLoginDisconnectPacket) packet).getReason());
            } else if (packet instanceof ClientboundLoginCompressionPacket) {
                viaSession.setCompressionThreshold(((ClientboundLoginCompressionPacket) packet).getThreshold());
            }
        } else if (protocol.getState() == ProtocolState.STATUS) {
            if (packet instanceof ClientboundStatusResponsePacket) {
                ServerStatusInfo info = ((ClientboundStatusResponsePacket) packet).getInfo();
                ServerInfoHandler handler = session.getFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, info);
                }

                session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
            } else if (packet instanceof ClientboundPongResponsePacket) {
                long time = System.currentTimeMillis() - ((ClientboundPongResponsePacket) packet).getPingTime();
                ServerPingTimeHandler handler = session.getFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, time);
                }

                session.disconnect("Finished");
            }
        } else if (protocol.getState() == ProtocolState.GAME) {
            if (packet instanceof ClientboundKeepAlivePacket && session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                session.send(new ServerboundKeepAlivePacket(((ClientboundKeepAlivePacket) packet).getPingId()));
            } else if (packet instanceof ClientboundDisconnectPacket) {
                session.disconnect(((ClientboundDisconnectPacket) packet).getReason());
            }
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        if (packet instanceof ClientIntentionPacket) {
            // Once the HandshakePacket has been sent, switch to the next protocol mode.
            MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
            protocol.setState(this.targetState);

            if (this.targetState == ProtocolState.LOGIN) {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                session.send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
            } else {
                session.send(new ServerboundStatusRequestPacket());
            }
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
        if (this.targetState == ProtocolState.LOGIN) {
            event.getSession().send(new ClientIntentionPacket(protocol.getCodec().getProtocolVersion(), event.getSession().getHost(), event.getSession().getPort(), HandshakeIntent.LOGIN));
        } else if (this.targetState == ProtocolState.STATUS) {
            event.getSession().send(new ClientIntentionPacket(protocol.getCodec().getProtocolVersion(), event.getSession().getHost(), event.getSession().getPort(), HandshakeIntent.STATUS));
        }
    }
}
