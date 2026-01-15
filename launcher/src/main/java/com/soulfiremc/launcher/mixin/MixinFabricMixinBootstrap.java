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

import com.soulfiremc.launcher.SFMinecraftDownloader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.FabricMixinBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnusedMixin")
@Mixin(FabricMixinBootstrap.class)
public class MixinFabricMixinBootstrap {
  @Redirect(method = "init",
    at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/impl/launch/FabricLauncher;isDevelopment()Z"),
    remap = false)
  private static boolean isDevelopment(FabricLauncher instance) {
    // Trigger refmap remapping at runtime if not running in intermediary
    return SFMinecraftDownloader.IS_OBFUSCATED_RELEASE && !"intermediary".equals(FabricLauncherBase.getLauncher().getMappingConfiguration().getRuntimeNamespace());
  }
}
