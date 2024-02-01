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
package net.pistonmaster.soulfire.server.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;

/**
 * This class is used to control the bot.
 * The goal is to reduce friction for doing simple things.
 */
@RequiredArgsConstructor
public class BotControlAPI {
    private final SessionDataManager sessionDataManager;
    private final SecureRandom secureRandom = new SecureRandom();
    @Getter
    @Setter
    private int sequenceNumber = 0;

    public boolean toggleFlight() {
        var abilitiesData = sessionDataManager.abilitiesData();
        if (abilitiesData != null && !abilitiesData.allowFlying()) {
            throw new IllegalStateException("You can't fly! (Server said so)");
        }

        var newFly = !sessionDataManager.controlState().flying();
        sessionDataManager.controlState().flying(newFly);

        // Let the server know we are flying
        sessionDataManager.sendPacket(new ServerboundPlayerAbilitiesPacket(newFly));

        return newFly;
    }

    public boolean toggleSprint() {
        var newSprint = !sessionDataManager.controlState().sprinting();
        sessionDataManager.controlState().sprinting(newSprint);

        // Let the server know we are sprinting
        sessionDataManager.sendPacket(new ServerboundPlayerCommandPacket(
                sessionDataManager.clientEntity().entityId(),
                newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING
        ));

        return newSprint;
    }

    public boolean toggleSneak() {
        var newSneak = !sessionDataManager.controlState().sneaking();
        sessionDataManager.controlState().sneaking(newSneak);

        // Let the server know we are sneaking
        sessionDataManager.sendPacket(new ServerboundPlayerCommandPacket(
                sessionDataManager.clientEntity().entityId(),
                newSneak ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING
        ));

        return newSneak;
    }

    public void sendMessage(String message) {
        var now = Instant.now();
        if (message.startsWith("/")) {
            var command = message.substring(1);
            // We only sign chat at the moment because commands require the entire command tree to be handled
            // Command signing is signing every string parameter in the command because of reporting /msg
            sessionDataManager.sendPacket(new ServerboundChatCommandPacket(
                    command,
                    now.toEpochMilli(),
                    0L,
                    Collections.emptyList(),
                    0,
                    new BitSet()
            ));
        } else {
            var salt = secureRandom.nextLong();
            sessionDataManager.sendPacket(new ServerboundChatPacket(
                    message,
                    now.toEpochMilli(),
                    salt,
                    null,
                    0,
                    new BitSet()
            ));
        }
    }

    public void registerPluginChannels(String... channels) {
        var buffer = Unpooled.buffer();
        for (int i = 0; i < channels.length; i++) {
            var channel = channels[i];
            buffer.writeBytes(channel.getBytes(StandardCharsets.UTF_8));

            if (i != channels.length - 1) {
                buffer.writeByte(0);
            }
        }

        sendPluginMessage("minecraft:register", buffer);
    }

    public void sendPluginMessage(String channel, ByteBuf data) {
        var array = new byte[data.readableBytes()];
        data.readBytes(array);

        sendPluginMessage(channel, array);
    }

    public void sendPluginMessage(String channel, byte[] data) {
        sessionDataManager.sendPacket(new ServerboundCustomPayloadPacket(
                channel,
                data
        ));
    }
}
