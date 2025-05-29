package com.soulfiremc.mod.mixin.rendering;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "add", at = @At("HEAD"), cancellable = true)
  private void addHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
  private void makeParticleHook(CallbackInfoReturnable<Particle> cir) {
    cir.setReturnValue(null);
  }

  @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
  private void crackHook(CallbackInfo cir) {
    cir.cancel();
  }

  @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
  private void destroyHook(CallbackInfo cir) {
    cir.cancel();
  }
}
