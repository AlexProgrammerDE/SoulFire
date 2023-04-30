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
package net.pistonmaster.serverwrecker.addons;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoRespawn implements InternalAddon, EventSubscriber<SWPacketReceiveEvent> {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListener(SWPacketReceiveEvent.class, this);
    }

    @Override
    public void on(@NonNull SWPacketReceiveEvent event) {
        BotConnection connection = event.getConnection();
        ServerWrecker serverWrecker = connection.serverWrecker();
        if (!connection.options().autoRespawn()) {
            return;
        }

        if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
            connection.logger().info("[AutoRespawn] Died with killer: {} and message: '{}'",
                    combatKillPacket.getKillerId(), serverWrecker.getMessageSerializer().serialize(combatKillPacket.getMessage()));
            connection.session().send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
        }
    }
}
