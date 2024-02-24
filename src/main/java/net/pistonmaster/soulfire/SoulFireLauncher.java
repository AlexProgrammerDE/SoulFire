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
package net.pistonmaster.soulfire;

import java.util.List;
import net.pistonmaster.soulfire.util.SFContextClassLoader;

/**
 * This class only changes the classLoader for the rest of the program. This is so we can merge
 * plugin and server classes.
 */
public class SoulFireLauncher {
  private static final SFContextClassLoader SF_CONTEXT_CLASS_LOADER = new SFContextClassLoader();

  public static void main(String[] args) {
    Thread.currentThread().setContextClassLoader(SF_CONTEXT_CLASS_LOADER);

    try {
      SF_CONTEXT_CLASS_LOADER
          .loadClass("net.pistonmaster.soulfire.SoulFireBootstrap")
          .getDeclaredMethod("bootstrap", String[].class, List.class)
          .invoke(null, args, SF_CONTEXT_CLASS_LOADER.childClassLoaders());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
