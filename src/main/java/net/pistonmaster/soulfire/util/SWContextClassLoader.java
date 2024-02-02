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
package net.pistonmaster.soulfire.util;

import lombok.Getter;
import net.lenni0451.reflect.Methods;
import net.lenni0451.reflect.exceptions.MethodInvocationException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SWContextClassLoader extends ClassLoader {
    @Getter
    private final List<ClassLoader> childClassLoaders = new ArrayList<>();
    private final Method findLoadedClassMethod;
    private final ClassLoader platformClassLoader = ClassLoader.getSystemClassLoader().getParent();

    public SWContextClassLoader() {
        super(ClassLoader.getSystemClassLoader());
        try {
            findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            var c = findLoadedClass(name);
            if (c == null) {
                try {
                    return Methods.invoke(platformClassLoader, findLoadedClassMethod, name, resolve);
                } catch (MethodInvocationException ignored) {
                }

                var classData = loadClassData(this.getParent(), name);
                if (classData == null) {
                    // Check if child class loaders can load the class
                    for (var childClassLoader : childClassLoaders) {
                        try {
                            var pluginClass = (Class<?>) Methods.invoke(childClassLoader, findLoadedClassMethod, name, resolve);
                            if (pluginClass != null) {
                                return pluginClass;
                            }
                        } catch (MethodInvocationException ignored) {
                        }
                    }

                    throw new ClassNotFoundException(name);
                }

                c = defineClass(name, classData, 0, classData.length);
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    private byte[] loadClassData(ClassLoader classLoader, String className) {
        var classPath = className.replace('.', '/') + ".class";

        try (var inputStream = classLoader.getResourceAsStream(classPath)) {
            if (inputStream == null) {
                return null;
            }

            return inputStream.readAllBytes();
        } catch (IOException ignored) {
            return null;
        }
    }
}
