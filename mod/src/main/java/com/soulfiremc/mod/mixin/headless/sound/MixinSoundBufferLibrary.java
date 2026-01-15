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
package com.soulfiremc.mod.mixin.headless.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(SoundBufferLibrary.class)
public class MixinSoundBufferLibrary {
  @Unique
  private static final Logger hmc_optimizations$LOGGER = LogUtils.getLogger();
  @Unique
  private static final CompletableFuture<?> hmc_optimizations$Future = new CompletableFuture<>();

  static {
    hmc_optimizations$LOGGER.info("Completing hmc_optimizations$Future");
    hmc_optimizations$Future.completeExceptionally(new IOException("HMC-Optimizations aborted sound loading."));
  }

  @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
  private void getCompleteBufferHook(Identifier identifier, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

  @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
  private void getStreamStreamHook(Identifier identifier, boolean looping, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

  @Inject(method = "preload", at = @At("HEAD"), cancellable = true)
  private void preloadHook(Collection<Sound> collection, CallbackInfoReturnable<CompletableFuture<?>> cir) {
    cir.setReturnValue(hmc_optimizations$Future);
  }

}
