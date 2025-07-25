/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.mod.mixin.soulfire.optimization;

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
