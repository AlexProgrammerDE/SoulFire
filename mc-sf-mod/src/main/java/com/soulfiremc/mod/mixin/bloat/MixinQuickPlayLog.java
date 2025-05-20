package com.soulfiremc.mod.mixin.bloat;

import net.minecraft.client.quickplay.QuickPlayLog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(QuickPlayLog.class)
public class MixinQuickPlayLog {
  @Final
  @Shadow
  private static QuickPlayLog INACTIVE;
  @Mutable
  @Final
  @Shadow
  private Path path;

  @Inject(method = "of", at = @At("HEAD"), cancellable = true)
  private static void of(String path, CallbackInfoReturnable<QuickPlayLog> cir) {
    cir.setReturnValue(INACTIVE);
  }
}
