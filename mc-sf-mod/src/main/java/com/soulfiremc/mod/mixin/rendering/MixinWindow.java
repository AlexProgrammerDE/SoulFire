package com.soulfiremc.mod.mixin.rendering;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {
  @Inject(method = "updateDisplay", at = @At("HEAD"), cancellable = true)
  private void updateDisplayHook(CallbackInfo ci) {
    ci.cancel();
  }
}
