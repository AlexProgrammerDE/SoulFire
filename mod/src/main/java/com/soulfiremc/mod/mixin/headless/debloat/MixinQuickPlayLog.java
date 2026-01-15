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
package com.soulfiremc.mod.mixin.headless.debloat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.quickplay.QuickPlayLog;
import org.objectweb.asm.Opcodes;
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

  @Inject(method = "of", at = @At("HEAD"), cancellable = true)
  private static void of(String path, CallbackInfoReturnable<QuickPlayLog> cir) {
    cir.setReturnValue(INACTIVE);
  }

  @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/quickplay/QuickPlayLog;path:Ljava/nio/file/Path;", opcode = Opcodes.PUTFIELD))
  private void init(QuickPlayLog log, Path path, Operation<Void> original) {
  }
}
