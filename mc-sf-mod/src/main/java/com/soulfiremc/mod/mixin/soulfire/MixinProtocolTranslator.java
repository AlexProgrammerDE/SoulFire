package com.soulfiremc.mod.mixin.soulfire;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtocolTranslator.class)
public class MixinProtocolTranslator {
  @Inject(method = "getTargetVersion()Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;", at = @At("HEAD"), cancellable = true)
  private static void getTargetVersion(CallbackInfoReturnable<ProtocolVersion> cir) {
    cir.setReturnValue(ProtocolVersion.v1_21_5);
  }
}
