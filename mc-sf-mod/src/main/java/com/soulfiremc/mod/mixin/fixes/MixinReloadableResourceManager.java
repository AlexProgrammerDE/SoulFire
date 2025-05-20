package com.soulfiremc.mod.mixin.fixes;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableResourceManager.class)
public class MixinReloadableResourceManager {
  @Inject(method = "registerReloadListener", at = @At("HEAD"), cancellable = true)
  private void registerReloadListenerHook(PreparableReloadListener listener, CallbackInfo ci) {
    ci.cancel();
  }
}
