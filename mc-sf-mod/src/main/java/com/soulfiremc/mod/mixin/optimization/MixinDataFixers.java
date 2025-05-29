package com.soulfiremc.mod.mixin.optimization;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixerBuilder;
import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(DataFixers.class)
public class MixinDataFixers {
  @Inject(method = "optimize", at = @At("HEAD"), cancellable = true)
  private static void optimize(Set<DSL.TypeReference> references, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(CompletableFuture.completedFuture(null));
  }

  @Inject(method = "addFixers", at = @At("HEAD"), cancellable = true)
  private static void addFixers(DataFixerBuilder builder, CallbackInfo ci) {
    ci.cancel();
  }
}
