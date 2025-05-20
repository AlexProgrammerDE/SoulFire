package com.soulfiremc.mod.mixin.bloat;

import com.mojang.realmsclient.gui.task.DataFetcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataFetcher.Subscription.class)
public class MixinDataFetcherSubscription {
  @Inject(method = "tick", cancellable = true, at = @At("HEAD"))
  private void tickHook(CallbackInfo ci) {
    // Cancel the tick method to prevent any data fetching
    ci.cancel();
  }
}
