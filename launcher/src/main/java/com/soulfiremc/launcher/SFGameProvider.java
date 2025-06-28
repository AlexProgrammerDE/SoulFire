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
package com.soulfiremc.launcher;

import lombok.SneakyThrows;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public class SFGameProvider extends MinecraftGameProvider {
  @SuppressWarnings("unchecked")
  @SneakyThrows
  @Override
  public boolean locateGame(FabricLauncher launcher, String[] args) {
    var logJarsField = MinecraftGameProvider.class.getDeclaredField("logJars");
    logJarsField.setAccessible(true);
    var logJars = (Set<Path>) logJarsField.get(this);
    LibClassifier<SFLibrary> classifier = new LibClassifier<>(SFLibrary.class, launcher.getEnvironmentType(), this);
    classifier.process(launcher.getClassPath());

    for (var lib : SFLibrary.LOGGING) {
      logJars.add(Objects.requireNonNull(classifier.getOrigin(lib)));
    }

    return super.locateGame(launcher, args);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
