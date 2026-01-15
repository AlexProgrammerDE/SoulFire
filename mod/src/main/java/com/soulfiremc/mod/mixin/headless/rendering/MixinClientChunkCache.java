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
package com.soulfiremc.mod.mixin.headless.rendering;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientChunkCache.class)
public class MixinClientChunkCache {
  // prevent LevelLightEngine from creating its engines.
  // Those engines queues would run full without a render event to poll from them
  @ModifyArg(
    method = "<init>",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;<init>(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V"),
    index = 1)
  private boolean shouldCreateBlockLightEngine(boolean shouldCreateBlockLightEngine) {
    return false;
  }

  @ModifyArg(
    method = "<init>",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;<init>(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V"),
    index = 2)
  private boolean shouldCreateSkyLightEngine(boolean shouldCreateSkylightEngine) {
    return false;
  }

}
