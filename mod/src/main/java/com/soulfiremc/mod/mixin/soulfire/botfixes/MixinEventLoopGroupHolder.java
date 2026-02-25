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

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.settings.instance.BotSettings;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EventLoopGroupHolder.class)
public class MixinEventLoopGroupHolder {
  @Unique
  private static final EventLoopGroupHolder IO_URING = new EventLoopGroupHolder("IoUring", IoUringSocketChannel.class, IoUringServerSocketChannel.class) {
    @Override
    protected @NonNull IoHandlerFactory ioHandlerFactory() {
      return IoUringIoHandler.newFactory();
    }
  };

  @Inject(method = "remote", at = @At("HEAD"), cancellable = true)
  private static void allowIoUring(boolean allowNativeTransport, CallbackInfoReturnable<EventLoopGroupHolder> cir) {
    var connection = BotConnection.current();
    if (allowNativeTransport
      && connection != null
      && connection.settingsSource().get(BotSettings.USE_IO_URING)
      && !BedrockProtocolVersion.bedrockLatest.equals(
      connection.settingsSource().get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER))
      && IoUring.isAvailable()) {
      cir.setReturnValue(IO_URING);
    }
  }
}
