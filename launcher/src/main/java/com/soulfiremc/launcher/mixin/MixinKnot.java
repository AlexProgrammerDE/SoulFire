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
package com.soulfiremc.launcher.mixin;

import net.fabricmc.loader.impl.launch.knot.Knot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnusedMixin")
@Mixin(Knot.class)
public class MixinKnot {
  @Redirect(method = "getTargetNamespace", at = @At(value = "FIELD", target = "Lnet/fabricmc/loader/impl/launch/knot/Knot;IS_DEVELOPMENT:Z", remap = false))
  private boolean isDevelopment() {
    // Always return true to force classpath loading
    return false; // TODO
  }
}
