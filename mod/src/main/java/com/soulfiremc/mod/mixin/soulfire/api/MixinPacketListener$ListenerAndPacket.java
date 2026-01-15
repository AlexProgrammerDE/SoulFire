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
package com.soulfiremc.mod.mixin.soulfire.api;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.settings.instance.BotSettings;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class MixinPacketListener$ListenerAndPacket {
  @Unique
  private static final Logger log = LoggerFactory.getLogger("PacketListener");

  @WrapOperation(method = "handle()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketListener;onPacketError(Lnet/minecraft/network/protocol/Packet;Ljava/lang/Exception;)V"))
  public void onPacketError(PacketListener instance, Packet<?> packet, Exception exception, Operation<Void> original) {
    if (BotConnection.CURRENT.get().settingsSource().get(BotSettings.IGNORE_PACKET_HANDLING_ERRORS)) {
      log.warn("Ignoring packet handling error", exception);
      return;
    }

    original.call(instance, packet, exception);
  }
}
