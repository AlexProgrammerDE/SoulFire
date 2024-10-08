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

import com.soulfiremc.server.util.structs.SFContextClassLoader;

import java.nio.file.Path;
import java.util.List;

public abstract class SoulFireAbstractLauncher {
  public void run(String[] args) {
    var contextClassLoader = new SFContextClassLoader(getLibrariesDirectory());
    Thread.currentThread().setContextClassLoader(contextClassLoader);

    try {
      contextClassLoader
        .loadClass(getBootstrapClassName())
        .getDeclaredMethod("bootstrap", String[].class, List.class)
        .invoke(null, args, contextClassLoader.childClassLoaders());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract String getBootstrapClassName();

  protected abstract Path getLibrariesDirectory();
}
