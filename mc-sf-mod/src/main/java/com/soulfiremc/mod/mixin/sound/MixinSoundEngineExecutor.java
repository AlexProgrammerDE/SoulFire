package com.soulfiremc.mod.mixin.sound;

import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngineExecutor.class)
public class MixinSoundEngineExecutor {
  @Inject(method = "run", at = @At("HEAD"), cancellable = true)
  private void run(CallbackInfo ci) {
    ci.cancel();
  }
}
