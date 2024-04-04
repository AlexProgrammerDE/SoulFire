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
package com.soulfiremc.generator.mixin;

import com.soulfiremc.generator.DataGenerators;
import com.soulfiremc.generator.Main;
import java.nio.file.Path;
import net.minecraft.DetectedVersion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class ReadyMixin {
  @Inject(method = "initServer()Z", at = @At("TAIL"))
  private void init(CallbackInfoReturnable<Boolean> cir) {
    Main.SERVER = (MinecraftServer) (Object) this;

    Main.LOGGER.info("Starting data generation!");
    var versionName = DetectedVersion.BUILT_IN.getName();
    var dataDumpDirectory =
      Path.of(System.getProperty("user.dir")).resolve("minecraft-data").resolve(versionName);
    var success = DataGenerators.runDataGenerators(dataDumpDirectory);
    Main.LOGGER.info("Done data generation! Success: {}", success);

    Runtime.getRuntime().halt(success ? 0 : 1);
  }
}
