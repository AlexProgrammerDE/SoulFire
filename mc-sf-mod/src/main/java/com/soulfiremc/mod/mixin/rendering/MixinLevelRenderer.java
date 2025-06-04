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
package com.soulfiremc.mod.mixin.rendering;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickHook(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
  private void setSectionDirty(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "onSectionBecomingNonEmpty", at = @At("HEAD"), cancellable = true)
  private void onSectionBecomingNonEmpty(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "isSectionCompiled", at = @At("HEAD"), cancellable = true)
  private void isSectionCompiledHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(true);
  }

  @Inject(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
  private void isSectionCompiledHook(CallbackInfoReturnable<Particle> cir) {
    cir.setReturnValue(null);
  }
}
