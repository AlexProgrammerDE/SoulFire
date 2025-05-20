package com.soulfiremc.mod.mixin.bloat;

import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSessionTelemetryManager.class)
public class MixinWorldSessionTelemetryManager {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickHook(CallbackInfo ci) {
    ci.cancel();
  }
}
