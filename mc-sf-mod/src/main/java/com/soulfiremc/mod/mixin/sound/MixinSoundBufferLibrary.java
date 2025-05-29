package com.soulfiremc.mod.mixin.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(SoundBufferLibrary.class)
public class MixinSoundBufferLibrary {
  @Unique
  private static final Logger hmc_optimizations$LOGGER = LogUtils.getLogger();
  @Unique
  private static final CompletableFuture<?> hmc_optimizations$Future = new CompletableFuture<>();

  static {
    hmc_optimizations$LOGGER.info("Completing hmc_optimizations$Future");
    hmc_optimizations$Future.completeExceptionally(new IOException("HMC-Optimizations aborted sound loading."));
  }

  @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
  private void getCompleteBufferHook(ResourceLocation resourceLocation, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

  @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
  private void getStreamStreamHook(ResourceLocation resourceLocation, boolean looping, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

  @Inject(method = "preload", at = @At("HEAD"), cancellable = true)
  private void preloadHook(Collection<Sound> collection, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

}
