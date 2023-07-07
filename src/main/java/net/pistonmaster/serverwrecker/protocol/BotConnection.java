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
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.event.UnregisterCleanup;
import net.pistonmaster.serverwrecker.protocol.bot.BotControlAPI;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;

public record BotConnection(BotConnectionFactory factory, ServerWrecker serverWrecker, SettingsHolder settingsHolder,
                            Logger logger, MinecraftProtocol protocol, ViaClientSession session,
                            BotConnectionMeta meta) {
    public boolean isOnline() {
        return session.isConnected();
    }

    public void disconnect() {
        session.disconnect("Disconnect");
    }

    public SessionDataManager sessionDataManager() {
        return meta.getSessionDataManager();
    }

    public BotControlAPI botControl() {
        return meta.getBotControlAPI();
    }

    public <T extends UnregisterCleanup> T cleanup(T instance) {
        meta.getUnregisterCleanups().add(instance);
        return instance;
    }

    public void tick(long ticks, float partialTicks) {
        session.tick(); // Ensure all packets are handled before ticking
        for (int i = 0; i < ticks; i++) {
            try {
                sessionDataManager().tick();
            } catch (Throwable t) {
                logger.error("Error while ticking bot!", t);
            }
        }
    }

    public GlobalTrafficShapingHandler getTrafficHandler() {
        return session.getFlag(SWProtocolConstants.TRAFFIC_HANDLER);
    }
}
