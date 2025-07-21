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

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnusedMixin")
@Mixin(FabricLoaderImpl.class)
public class MixinFabricLoaderImpl {
  @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/impl/FabricLoaderImpl;isDevelopmentEnvironment()Z"))
  private boolean redirectIsDevelopmentEnvironment(FabricLoaderImpl instance) {
    // Trigger mod remapping at runtime if not running in intermediary
    return !FabricLauncherBase.getLauncher().getMappingConfiguration().getRuntimeNamespace().equals("intermediary");
  }
}
