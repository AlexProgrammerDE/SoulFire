package com.soulfiremc.mod.mixin.rendering;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void renderHook(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickHook(CallbackInfo ci) {
    ci.cancel();
  }

}
