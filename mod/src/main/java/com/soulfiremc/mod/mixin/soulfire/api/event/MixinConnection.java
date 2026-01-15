/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.mod.mixin.soulfire.api.event;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.DummyPacket;
import com.soulfiremc.server.api.event.bot.BotPacketPreReceiveEvent;
import com.soulfiremc.server.api.event.bot.BotPacketPreSendEvent;
import com.soulfiremc.server.bot.BotConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;

@Mixin(Connection.class)
public class MixinConnection {
  @Shadow
  private Channel channel;

  @SuppressWarnings("unchecked")
  @ModifyVariable(method = "genericsFtw", at = @At("HEAD"), argsOnly = true)
  private static Packet<?> handlePacket(Packet<?> parentPacket) {
    var connection = BotConnection.CURRENT.get();
    if (parentPacket instanceof ClientboundBundlePacket bundlePacket) {
      var subPackets = new ArrayList<Packet<? super ClientGamePacketListener>>();

      for (var subPacket : bundlePacket.subPackets()) {
        var event = new BotPacketPreReceiveEvent(connection, subPacket);
        SoulFireAPI.postEvent(event);

        if (event.packet() != null) {
          subPackets.add((Packet<? super ClientGamePacketListener>) event.packet());
        }
      }

      return new ClientboundBundlePacket(subPackets);
    } else {
      var event = new BotPacketPreReceiveEvent(connection, parentPacket);
      SoulFireAPI.postEvent(event);

      if (event.packet() != null) {
        return event.packet();
      } else {
        return new DummyPacket();
      }
    }
  }

  @WrapMethod(method = "doSendPacket")
  public void wrapDoSendPacket(Packet<?> packet, @Nullable ChannelFutureListener sendListener, boolean flush, Operation<Void> original) {
    var connection = BotConnection.CURRENT.get();
    var event = new BotPacketPreSendEvent(connection, packet);
    SoulFireAPI.postEvent(event);

    if (event.packet() != null) {
      original.call(event.packet(), sendListener, flush);
    } else {
      if (sendListener != null) {
        try {
          sendListener.operationComplete(channel.newSucceededFuture());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      if (flush) {
        channel.flush();
      }
    }
  }
}
