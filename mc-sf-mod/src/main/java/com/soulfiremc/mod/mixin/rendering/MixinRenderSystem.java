package com.soulfiremc.mod.mixin.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
  @Inject(method = "isOnRenderThread", at = @At("HEAD"), cancellable = true)
  private static void isOnRenderThreadHook(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(true);
  }
}
