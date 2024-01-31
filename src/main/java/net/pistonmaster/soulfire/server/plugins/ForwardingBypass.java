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
package net.pistonmaster.soulfire.server.plugins;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.SoulFireServer;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.ComboProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.settings.lib.property.StringProperty;
import net.pistonmaster.soulfire.server.util.UUIDHelper;
import net.pistonmaster.soulfire.server.util.VelocityConstants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ForwardingBypass implements InternalExtension {
    private static final char LEGACY_FORWARDING_SEPARATOR = '\0';

    private static int findForwardingVersion(int requested, BotConnection player) {
        // TODO: Fix this
        /*
        // Ensure we are in range
        requested = Math.min(requested, VelocityConstants.MODERN_FORWARDING_MAX_VERSION);
        if (requested > VelocityConstants.MODERN_FORWARDING_DEFAULT) {
            if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
                return requested >= VelocityConstants.MODERN_LAZY_SESSION
                        ? VelocityConstants.MODERN_LAZY_SESSION
                        : VelocityConstants.MODERN_FORWARDING_DEFAULT;
            }
            if (player.getIdentifiedKey() != null) {
                // No enhanced switch on java 11
                return switch (player.getIdentifiedKey().getKeyRevision()) {
                    case GENERIC_V1 -> VelocityConstants.MODERN_FORWARDING_WITH_KEY;
                    // Since V2 is not backwards compatible we have to throw the key if v2 and requested is v1
                    case LINKED_V2 -> requested >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                            ? VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                            : VelocityConstants.MODERN_FORWARDING_DEFAULT;
                    default -> VelocityConstants.MODERN_FORWARDING_DEFAULT;
                };
            } else {
                return VelocityConstants.MODERN_FORWARDING_DEFAULT;
            }
        }
         */
        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
    }

    private static ByteBuf createForwardingData(String hmacSecret, String address,
                                                BotConnection player, int requestedVersion) {
        var forwarded = Unpooled.buffer(2048);
        try {
            var actualVersion = findForwardingVersion(requestedVersion, player);

            var codecHelper = player.session().getCodecHelper();
            codecHelper.writeVarInt(forwarded, actualVersion);
            codecHelper.writeString(forwarded, address);
            codecHelper.writeUUID(forwarded, player.meta().minecraftAccount().uniqueId());
            codecHelper.writeString(forwarded, player.meta().minecraftAccount().username());

            // TODO: Fix this
            /*
            // This serves as additional redundancy. The key normally is stored in the
            // login start to the server, but some setups require this.
            if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY
                    && actualVersion < VelocityConstants.MODERN_LAZY_SESSION) {
                IdentifiedKey key = player.getIdentifiedKey();
                assert key != null;
                codecHelper.writePlayerKey(forwarded, key);

                // Provide the signer UUID since the UUID may differ from the
                // assigned UUID. Doing that breaks the signatures anyway but the server
                // should be able to verify the key independently.
                if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2) {
                    if (key.getSignatureHolder() != null) {
                        forwarded.writeBoolean(true);
                        ProtocolUtils.writeUuid(forwarded, key.getSignatureHolder());
                    } else {
                        // Should only not be provided if the player was connected
                        // as offline-mode and the signer UUID was not backfilled
                        forwarded.writeBoolean(false);
                    }
                }
            }
             */

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
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ForwardingBypassSettings.class, "Forwarding Bypass");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(ForwardingBypass.class);
        PluginHelper.registerBotEventConsumer(SWPacketSendingEvent.class, this::onPacket);
        PluginHelper.registerBotEventConsumer(SWPacketReceiveEvent.class, this::onPacketReceive);
    }

    public void onPacket(SWPacketSendingEvent event) {
        if (!(event.packet() instanceof ClientIntentionPacket handshake)) {
            return;
        }

        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        var hostname = handshake.getHostname();
        var uuid = connection.meta().minecraftAccount().uniqueId();

        switch (settingsHolder.get(ForwardingBypassSettings.FORWARDING_MODE, ForwardingBypassSettings.ForwardingMode.class)) {
            case LEGACY -> event.packet(handshake
                    .withHostname(createLegacyForwardingAddress(uuid, getForwardedIp(), hostname)));
            case BUNGEE_GUARD -> event.packet(handshake
                    .withHostname(createBungeeGuardForwardingAddress(uuid, getForwardedIp(), hostname, settingsHolder.get(ForwardingBypassSettings.SECRET))));
        }
    }

    public void onPacketReceive(SWPacketReceiveEvent event) {
        if (!(event.packet() instanceof ClientboundCustomQueryPacket loginPluginMessage)) {
            return;
        }

        if (!loginPluginMessage.getChannel().equals("velocity:player_info")) {
            return;
        }

        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (settingsHolder.get(ForwardingBypassSettings.FORWARDING_MODE, ForwardingBypassSettings.ForwardingMode.class)
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

        var forwardingData = createForwardingData(settingsHolder.get(ForwardingBypassSettings.SECRET),
                getForwardedIp(), connection,
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
    private String createLegacyForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname, UnaryOperator<List<GameProfile.Property>> propertiesTransform) {
        // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
        // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
        // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
        var data = new StringBuilder(initialHostname)
                .append(LEGACY_FORWARDING_SEPARATOR)
                .append(selfIp)
                .append(LEGACY_FORWARDING_SEPARATOR)
                .append(UUIDHelper.convertToNoDashes(botUniqueId))
                .append(LEGACY_FORWARDING_SEPARATOR);
        SoulFireServer.GENERAL_GSON
                .toJson(propertiesTransform.apply(List.of()), data);
        return data.toString();
    }

    private String createLegacyForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname) {
        return createLegacyForwardingAddress(botUniqueId, selfIp, initialHostname, UnaryOperator.identity());
    }

    private String createBungeeGuardForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname, String forwardingSecret) {
        // Append forwarding secret as a BungeeGuard token.
        var property = new GameProfile.Property("bungeeguard-token", forwardingSecret, "");
        return createLegacyForwardingAddress(
                botUniqueId,
                selfIp,
                initialHostname,
                properties -> ImmutableList.<GameProfile.Property>builder().addAll(properties).add(property).build());
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ForwardingBypassSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("forwarding-bypass");
        public static final ComboProperty FORWARDING_MODE = BUILDER.ofEnum(
                "forwarding-mode",
                "Forwarding mode",
                new String[]{"--forwarding-mode"},
                "What type of forwarding to use",
                ForwardingMode.values(),
                ForwardingMode.NONE
        );
        public static final StringProperty SECRET = BUILDER.ofStringSecret(
                "secret",
                "Secret",
                new String[]{"--secret"},
                "Secret key used for forwarding. (Not needed for legacy mode)",
                "forwarding secret"
        );

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
