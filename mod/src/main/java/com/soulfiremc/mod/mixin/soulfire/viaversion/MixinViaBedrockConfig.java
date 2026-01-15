/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.mod.mixin.soulfire.viaversion;

import net.raphimc.viabedrock.ViaBedrockConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViaBedrockConfig.class)
public final class MixinViaBedrockConfig {
  @Inject(method = "shouldTranslateResourcePacks", at = @At("HEAD"), cancellable = true)
  public void shouldTranslateResourcePacks(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(false);
  }

  @Inject(method = "getBlobCacheMode", at = @At("HEAD"), cancellable = true)
  public void getBlobCacheMode(CallbackInfoReturnable<ViaBedrockConfig.BlobCacheMode> cir) {
    cir.setReturnValue(ViaBedrockConfig.BlobCacheMode.MEMORY);
  }

  @Inject(method = "getPackCacheMode", at = @At("HEAD"), cancellable = true)
  public void getPackCacheMode(CallbackInfoReturnable<ViaBedrockConfig.PackCacheMode> cir) {
    cir.setReturnValue(ViaBedrockConfig.PackCacheMode.MEMORY);
  }
}
