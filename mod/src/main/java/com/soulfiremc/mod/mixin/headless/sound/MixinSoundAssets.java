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
package com.soulfiremc.mod.mixin.headless.sound;

import com.soulfiremc.server.renderer.PovSoundAssets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FixedPathPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Mixin(FixedPathPackResources.class)
public class MixinSoundAssets {
  @Shadow @Final private Map<PackType, List<Path>> pathsForType;

  @Unique
  private boolean soulfire$indexedClientAssets(PackType type) {
    return type == PackType.CLIENT_RESOURCES && pathsForType.getOrDefault(type, List.of()).stream()
      .anyMatch(path -> path.getFileSystem() instanceof LinkFileSystem);
  }

  @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
  private void lazySoundResource(PackType type, Identifier location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
    if (cir.getReturnValue() == null && soulfire$indexedClientAssets(type)) cir.setReturnValue(PovSoundAssets.shared().resource(location));
  }

  @Inject(method = "listResources", at = @At("RETURN"))
  private void listLazySounds(PackType type, String namespace, String directory, PackResources.ResourceOutput output, CallbackInfo ci) {
    if (soulfire$indexedClientAssets(type)) PovSoundAssets.shared().list(namespace, directory, output);
  }
}
