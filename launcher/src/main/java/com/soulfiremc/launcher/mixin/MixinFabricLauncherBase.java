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
package com.soulfiremc.launcher.mixin;

import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricLauncherBase.class)
public class MixinFabricLauncherBase {
  /// @reason Rethrow, so we handle errors during launch ourselves.
  ///         Avoids issues with log4j async logging.
  @Inject(method = "handleFormattedException", at = @At("HEAD"))
  private static void handleFormattedException(FormattedException exc, CallbackInfo ci) {
    throw new RuntimeException(exc);
  }

  /// @reason We use our own uncaught exception handler.
  ///         We don't want Fabric to override it.
  @Inject(method = "setupUncaughtExceptionHandler", at = @At("HEAD"), cancellable = true)
  private static void setupUncaughtExceptionHandler(CallbackInfo ci) {
    ci.cancel();
  }
}
