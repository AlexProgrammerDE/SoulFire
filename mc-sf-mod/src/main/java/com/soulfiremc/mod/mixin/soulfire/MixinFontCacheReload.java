package com.soulfiremc.mod.mixin.soulfire;

import com.viaversion.viafabricplus.features.font.FontCacheReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontCacheReload.class)
public class MixinFontCacheReload {
  @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
  private static void reloadHook(CallbackInfo ci) {
    ci.cancel();
  }
}
