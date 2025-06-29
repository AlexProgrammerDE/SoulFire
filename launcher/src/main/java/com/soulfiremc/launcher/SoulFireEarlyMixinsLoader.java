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

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
public class SoulFireEarlyMixinsLoader {
  public static void injectEarlyMixins() {
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
}
