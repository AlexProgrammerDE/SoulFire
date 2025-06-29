package com.soulfiremc.launcher.mixin;

import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnusedMixin")
@Mixin(ClasspathModCandidateFinder.class)
public class MixinClasspathModCandidateFinder {
  @Redirect(method = "findCandidates", at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/impl/launch/FabricLauncher;isDevelopment()Z"), remap = false)
  private boolean isDevelopment(final FabricLauncher instance) {
    // Always return true to force classpath loading
    return true;
  }
}
