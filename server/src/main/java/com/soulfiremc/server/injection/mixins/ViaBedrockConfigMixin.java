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
package com.soulfiremc.server.injection.mixins;

import net.raphimc.viabedrock.ViaBedrockConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ViaBedrockConfig.class)
public class ViaBedrockConfigMixin {
  @Overwrite
  public boolean shouldTranslateResourcePacks() {
    return false;
  }

  @Overwrite
  public ViaBedrockConfig.BlobCacheMode getBlobCacheMode() {
    return ViaBedrockConfig.BlobCacheMode.MEMORY;
  }

  @Overwrite
  public ViaBedrockConfig.PackCacheMode getPackCacheMode() {
    return ViaBedrockConfig.PackCacheMode.MEMORY;
  }
}
