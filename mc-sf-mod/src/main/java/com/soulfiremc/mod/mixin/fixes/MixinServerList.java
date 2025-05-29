package com.soulfiremc.mod.mixin.fixes;

import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public class MixinServerList {
  @Inject(method = "saveSingleServer", at = @At("HEAD"), cancellable = true)
  private static void saveSingleServerHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "save", at = @At("HEAD"), cancellable = true)
  private void saveHook(CallbackInfo ci) {
    // Prevent saving the server list to avoid bloat
    ci.cancel();
  }
}
