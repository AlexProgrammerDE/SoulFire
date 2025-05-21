package com.soulfiremc.mod.mixin.bloat;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(RealmsClient.class)
public class MixinRealmsClient {
  @Inject(method = "execute", at = @At("HEAD"))
  public void execute(CallbackInfoReturnable<String> cir) throws RealmsServiceException {
    throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse("Not compatible"));
  }

  @Inject(method = "fetchFeatureFlags", at = @At("HEAD"), cancellable = true)
  public void fetchFeatureFlags(CallbackInfoReturnable<Set<String>> cir) {
    cir.setReturnValue(Set.of());
  }
}
