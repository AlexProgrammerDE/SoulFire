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
package com.soulfiremc.mod.mixin.soulfire.viaversion;

import com.soulfiremc.server.protocol.BotConnection;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtocolTranslator.class)
public class MixinProtocolTranslator {
  @Inject(method = "getTargetVersion()Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;", at = @At("HEAD"), cancellable = true)
  private static void getTargetVersion(CallbackInfoReturnable<ProtocolVersion> cir) {
    var botConnection = BotConnection.CURRENT.get();
    if (botConnection != null) {
      cir.setReturnValue(botConnection.currentProtocolVersion());
    }
  }

  @Inject(method = "setTargetVersion(Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;Z)V", at = @At("HEAD"), cancellable = true)
  private static void setTargetVersion(ProtocolVersion newVersion, boolean revertOnDisconnect, CallbackInfo ci) {
    var botConnection = BotConnection.CURRENT.get();
    if (botConnection != null) {
      botConnection.currentProtocolVersion(newVersion);
      ci.cancel();
    }
  }
}
