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
package com.soulfiremc.mod.mixin.soulfire.api;

import com.soulfiremc.server.protocol.BotConnection;
import io.netty.channel.Channel;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {
  @Shadow
  @Nullable
  private volatile PacketListener packetListener;
  @Shadow
  private Channel channel;

  @Inject(method = "handleDisconnection", at = @At("RETURN"))
  public void onDisconnect(CallbackInfo ci) {
    if (this.channel != null && !this.channel.isOpen()) {
      if (packetListener instanceof ClientCommonPacketListenerImpl commonListener
        && commonListener.isTransferring) {
        // Transfers aren't ordinary disconnects, we just ignore these
        return;
      }

      BotConnection.CURRENT.get().disconnect();
    }
  }
}
