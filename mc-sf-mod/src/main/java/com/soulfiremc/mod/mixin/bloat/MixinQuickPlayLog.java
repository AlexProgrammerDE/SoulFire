package com.soulfiremc.mod.mixin.bloat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.quickplay.QuickPlayLog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
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

  @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/quickplay/QuickPlayLog;path:Ljava/nio/file/Path;"))
  private void init(QuickPlayLog log, Path path, Operation<Void> original) {
  }

  @Inject(method = "of", at = @At("HEAD"), cancellable = true)
  private static void of(String path, CallbackInfoReturnable<QuickPlayLog> cir) {
    cir.setReturnValue(INACTIVE);
  }
}
