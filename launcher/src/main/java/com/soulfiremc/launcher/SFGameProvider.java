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
