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

import net.pistonmaster.soulfire.util.SWContextClassLoader;

import java.util.List;

/**
 * This class only changes the classLoader for the rest of the program.
 * This is so we can merge plugin and server classes.
 */
public class SoulFireLauncher {
    private static final SWContextClassLoader SW_CONTEXT_CLASS_LOADER = new SWContextClassLoader();

    public static void main(String[] args) {
        Thread.currentThread().setContextClassLoader(SW_CONTEXT_CLASS_LOADER);

        try {
            SW_CONTEXT_CLASS_LOADER.loadClass("net.pistonmaster.soulfire.SoulFireBootstrap")
                    .getDeclaredMethod("bootstrap", String[].class, List.class)
                    .invoke(null, args, SW_CONTEXT_CLASS_LOADER.childClassLoaders());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
