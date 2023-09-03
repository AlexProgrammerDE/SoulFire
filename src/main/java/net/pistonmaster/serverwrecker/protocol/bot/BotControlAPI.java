/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.model.AbilitiesData;

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
    private final BotMovementManager botMovementManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean toggleFlight() {
        AbilitiesData abilitiesData = sessionDataManager.getAbilitiesData();
        if (abilitiesData != null && !abilitiesData.allowFlying()) {
            throw new IllegalStateException("You can't fly! (Server said so)");
        }

        boolean newFly = !botMovementManager.getControlState().isFlying();
        botMovementManager.getControlState().setFlying(newFly);

        // Let the server know we are flying
        sessionDataManager.getSession().send(new ServerboundPlayerAbilitiesPacket(newFly));

        return newFly;
    }

    public boolean toggleSprint() {
        boolean newSprint = !botMovementManager.getControlState().isSprinting();
        botMovementManager.getControlState().setSprinting(newSprint);

        // Let the server know we are sprinting
        sessionDataManager.getSession().send(new ServerboundPlayerCommandPacket(
                sessionDataManager.getLoginData().entityId(),
                newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING
        ));

        return newSprint;
    }

    public boolean toggleSneak() {
        boolean newSneak = !botMovementManager.getControlState().isSneaking();
        botMovementManager.setSneaking(newSneak);

        // Let the server know we are sneaking
        sessionDataManager.getSession().send(new ServerboundPlayerCommandPacket(
                sessionDataManager.getLoginData().entityId(),
                newSneak ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING
        ));

        return newSneak;
    }

    public void sendMessage(String message) {
        Instant now = Instant.now();
        if (message.startsWith("/")) {
            String command = message.substring(1);
            // We only sign chat at the moment because commands require the entire command tree to be handled
            // Command signing is signing every string parameter in the command because of reporting /msg
            sessionDataManager.getSession().send(new ServerboundChatCommandPacket(
                    command,
                    now.toEpochMilli(),
                    0L,
                    Collections.emptyList(),
                    0,
                    new BitSet()
            ));
        } else {
            long salt = secureRandom.nextLong();
            sessionDataManager.getSession().send(new ServerboundChatPacket(
                    message,
                    now.toEpochMilli(),
                    salt,
                    null,
                    0,
                    new BitSet()
            ));
        }
    }
}
