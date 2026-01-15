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
