package com.soulfiremc.mod.mixin.optimization;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
  @Inject(method = "handleUpdateAdvancementsPacket", at = @At("HEAD"), cancellable = true)
  public void handleUpdateAdvancementsPacketHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "handleSelectAdvancementsTab", at = @At("HEAD"), cancellable = true)
  public void handleSelectAdvancementsTabHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "handleAwardStats", at = @At("HEAD"), cancellable = true)
  public void handleAwardStatsHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "handleRecipeBookAdd", at = @At("HEAD"), cancellable = true)
  public void handleRecipeBookAddHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "handleRecipeBookRemove", at = @At("HEAD"), cancellable = true)
  public void handleRecipeBookRemoveHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "handleRecipeBookSettings", at = @At("HEAD"), cancellable = true)
  public void handleRecipeBookSettingsHook(CallbackInfo ci) {
    ci.cancel();
  }
}
