package com.soulfiremc.mod.mixin.soulfire;

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
