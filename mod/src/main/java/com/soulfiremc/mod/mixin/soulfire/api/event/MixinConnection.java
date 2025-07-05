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
package com.soulfiremc.mod.mixin.soulfire.api.event;

import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotPacketReceiveEvent;
import com.soulfiremc.server.protocol.BotConnection;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {
  @Inject(method = "genericsFtw", at = @At("HEAD"))
  private static void handlePacket(Packet<?> parentPacket, PacketListener listener, CallbackInfo ci) {
    var connection = BotConnection.CURRENT.get();
    if (parentPacket instanceof BundlePacket<?> bundlePacket) {
      for (var packet : bundlePacket.subPackets()) {
        SoulFireAPI.postEvent(new BotPacketReceiveEvent(connection, packet));
      }
    } else {
      SoulFireAPI.postEvent(new BotPacketReceiveEvent(connection, parentPacket));
    }
  }
}
