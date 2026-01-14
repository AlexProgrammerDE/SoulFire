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

import com.soulfiremc.launcher.SFMinecraftDownloader;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnusedMixin")
@Mixin(MappingConfiguration.class)
public class MixinMappingConfiguration {
  @Inject(method = "getRuntimeNamespace",
    at = @At("HEAD"),
    cancellable = true,
    remap = false)
  public void getRuntimeNamespace(CallbackInfoReturnable<String> cir) {
    if (SFMinecraftDownloader.IS_OBFUSCATED_RELEASE && Boolean.getBoolean("sf.customIntermediaryDeobfuscation")) {
      // Temporarily remap to intermediary
      cir.setReturnValue("intermediary");
    }
  }
}
