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
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings("UnusedMixin")
@Mixin(MinecraftGameProvider.class)
public class MixinMinecraftGameProvider {
  @Redirect(method = "initialize", at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/impl/game/GameProviderHelper;deobfuscate(Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/nio/file/Path;Lnet/fabricmc/loader/impl/launch/FabricLauncher;)Ljava/util/Map;"))
  private Map<String, Path> redirectDeobfuscate(Map<String, Path> inputFileMap, String sourceNamespace, String gameId, String gameVersion, Path gameDir, FabricLauncher launcher) {
    // Inject custom logic to also deobfuscate to intermediary
    if (SFMinecraftDownloader.IS_OBFUSCATED_RELEASE && !"intermediary".equals(launcher.getMappingConfiguration().getRuntimeNamespace())) {
      IO.println("Remapping Minecraft to intermediary to remap mods. This will take a while...");
      System.setProperty("sf.customIntermediaryDeobfuscation", "true");
      GameProviderHelper.deobfuscate(
        inputFileMap,
        sourceNamespace,
        gameId,
        gameVersion,
        gameDir,
        launcher
      );
      System.clearProperty("sf.customIntermediaryDeobfuscation");
    }

    if (SFMinecraftDownloader.IS_OBFUSCATED_RELEASE) {
      System.out.printf("Remapping Minecraft to %s for runtime. This will take a while...%n", launcher.getMappingConfiguration().getRuntimeNamespace());
    }

    return GameProviderHelper.deobfuscate(
      inputFileMap,
      sourceNamespace,
      gameId,
      gameVersion,
      gameDir,
      launcher
    );
  }
}
