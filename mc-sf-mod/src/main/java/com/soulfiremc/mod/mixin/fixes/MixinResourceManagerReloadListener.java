package com.soulfiremc.mod.mixin.fixes;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ResourceManagerReloadListener.class)
public interface MixinResourceManagerReloadListener {
  /**
   * @author SoulFireMC
   * @reason Disable resource pack reloads
   */
  @Overwrite
  default CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
    return CompletableFuture.completedFuture(null);
  }
}
