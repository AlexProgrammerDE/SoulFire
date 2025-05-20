package com.soulfiremc.mod.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.soulfiremc.mod.util.SFModThreadLocals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
  @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
  private static void getInstance(CallbackInfoReturnable<Minecraft> cir) {
    var currentInstance = SFModThreadLocals.MINECRAFT_INSTANCE.get();
    if (currentInstance == null) {
      new RuntimeException().printStackTrace();
    } else {
      cir.setReturnValue(currentInstance);
    }
  }

  @Inject(method = "createUserApiService", at = @At("HEAD"), cancellable = true)
  private void createUserApiServiceHook(CallbackInfoReturnable<UserApiService> cir) {
    cir.setReturnValue(UserApiService.OFFLINE);
  }

  @Inject(method = "updateLevelInEngines", at = @At("HEAD"), cancellable = true)
  private void updateLevelEngineHook(ClientLevel level, CallbackInfo ci) {
    ci.cancel();
  }
}
