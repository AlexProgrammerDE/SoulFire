package com.soulfiremc.mod.mixin.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class MixinRenderTarget {

  @Inject(method = "blitToScreen", at = @At("HEAD"), cancellable = true)
  private void blitToScreenHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "blitAndBlendToTexture", at = @At("HEAD"), cancellable = true)
  private void blitAndBlendToTextureHook(CallbackInfo ci) {
    ci.cancel();
  }

}
