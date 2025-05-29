package com.soulfiremc.mod.mixin.rendering;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
  @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
  private void grabMouseHook(CallbackInfo ci) {
    ci.cancel();
  }
}
