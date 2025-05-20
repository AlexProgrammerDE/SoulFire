package com.soulfiremc.mod.mixin.fixes;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ResourceManagerReloadListener.class)
public interface MixinResourceManagerReloadListener {
  @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
  default void reload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
    cir.setReturnValue(CompletableFuture.completedFuture(null));
  }
}
