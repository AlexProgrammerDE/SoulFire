package com.soulfiremc.mod.mixin.rendering;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
  @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
  private void renderTransparentBackgroundHook(GuiGraphics guiGraphics, CallbackInfo ci) {
    ci.cancel();
  }

}
