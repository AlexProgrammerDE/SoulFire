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
package com.soulfiremc.mod.mixin.soulfire.botfixes;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.settings.instance.BotSettings;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.TimeUnit;

@Mixin(value = Connection.class)
public class MixinConnection {
  @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"))
  private static AbstractBootstrap<?, ?> useCustomGroup(Bootstrap instance, EventLoopGroup eventLoopGroup, Operation<AbstractBootstrap<Bootstrap, Channel>> original) {
    return original.call(instance, eventLoopGroup)
      .attr(SFConstants.NETTY_BOT_CONNECTION, BotConnection.CURRENT.get())
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(BotConnection.CURRENT.get().settingsSource().get(BotSettings.CONNECT_TIMEOUT)));
  }

  @WrapMethod(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V")
  private void injectLogIntoPacketHandler(ChannelHandlerContext context, Packet<?> packet, Operation<Void> original) {
    var botConnection = context.channel().attr(SFConstants.NETTY_BOT_CONNECTION).get();
    botConnection.runnableWrapper().runWrapped(() -> original.call(context, packet));
  }
}
