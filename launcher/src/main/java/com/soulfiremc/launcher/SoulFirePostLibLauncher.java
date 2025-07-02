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

import com.soulfiremc.shared.Base64Helpers;
import com.soulfiremc.shared.SFInfoPlaceholder;
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SoulFirePostLibLauncher {
  private static void injectEarlyMixins() {
    var classProvider = new CustomClassProvider(List.of(SoulFireAbstractLauncher.class.getClassLoader()));
    var transformerManager = new TransformerManager(classProvider);
    transformerManager.addTransformerPreprocessor(new MixinsTranslator());
    transformerManager.addTransformer("com.soulfiremc.launcher.mixin.*");

    try {
      transformerManager.hookInstrumentation(Agents.getInstrumentation());
      System.out.println("Used Runtime Agent to inject mixins");
    } catch (IOException t) {
      throw new IllegalStateException("Failed to inject mixins", t);
    }
  }

  @SuppressWarnings("unused")
  public static void runPostLib(Path basePath, String bootstrapClassName, String[] args) {
    System.setProperty("sf.baseDir", basePath.toAbsolutePath().toString());
    System.setProperty("java.awt.headless", "true");
    System.setProperty("joml.nounsafe", "true");
    System.setProperty(SystemProperties.SKIP_MC_PROVIDER, "true");
    System.setProperty("sf.bootstrap.class", bootstrapClassName);
    System.setProperty("sf.initial.arguments", Base64Helpers.joinBase64(args));

    injectEarlyMixins();
    SFInfoPlaceholder.register();
    SFMinecraftDownloader.loadAndInjectMinecraftJar(basePath);

    KnotClient.main(new String[0]);
  }
}
