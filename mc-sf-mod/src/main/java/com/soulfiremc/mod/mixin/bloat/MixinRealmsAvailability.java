package com.soulfiremc.mod.mixin.bloat;

import com.mojang.realmsclient.RealmsAvailability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(RealmsAvailability.class)
public class MixinRealmsAvailability {
  @Inject(method = "get", at = @At("HEAD"), cancellable = true)
  private static void getHook(CallbackInfoReturnable<CompletableFuture<RealmsAvailability.Result>> cir) {
    cir.setReturnValue(CompletableFuture.completedFuture(new RealmsAvailability.Result(RealmsAvailability.Type.INCOMPATIBLE_CLIENT, null)));
  }
}
