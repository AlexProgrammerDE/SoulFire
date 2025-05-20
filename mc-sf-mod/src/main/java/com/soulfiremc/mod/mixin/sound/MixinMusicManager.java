package com.soulfiremc.mod.mixin.sound;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public class MixinMusicManager {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickHook(CallbackInfo ci) {
    ci.cancel();
  }

}
