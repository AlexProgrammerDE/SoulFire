package com.soulfiremc.mod.mixin.soulfire;

import org.hibernate.boot.registry.classloading.internal.AggregatedClassLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AggregatedClassLoader.class)
public class MixinAggregatedClassLoader {
  @Inject(method = "locateSystemClassLoader", at = @At("HEAD"), cancellable = true)
  private static void locateSystemClassLoader(CallbackInfoReturnable<ClassLoader> cir) {
    cir.setReturnValue(null);
  }
}
