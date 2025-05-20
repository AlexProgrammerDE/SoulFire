package com.soulfiremc.mod.mixin.rendering;

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
