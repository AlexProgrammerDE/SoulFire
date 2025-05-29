package com.soulfiremc.mod.mixin.fixes;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {
  @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
  public void addMessageHook(GuiMessage message, CallbackInfo ci) {
    ci.cancel();
  }
}
