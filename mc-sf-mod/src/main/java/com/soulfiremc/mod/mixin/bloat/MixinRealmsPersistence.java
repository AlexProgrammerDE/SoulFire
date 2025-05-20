package com.soulfiremc.mod.mixin.bloat;

import com.mojang.realmsclient.util.RealmsPersistence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RealmsPersistence.class)
public class MixinRealmsPersistence {
  @Inject(method = "read", at = @At("HEAD"), cancellable = true)
  public void read(CallbackInfoReturnable<RealmsPersistence.RealmsPersistenceData> cir) {
    cir.setReturnValue(new RealmsPersistence.RealmsPersistenceData());
  }
}
