/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.util;

import com.github.steveice10.packetlib.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BusInvoker {
    private final Object bus;
    private final Map<Class<?>, MethodHandle> handlers = new HashMap<>();

    public BusInvoker(Object bus) {
        this.bus = bus;
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();

        for (Method declaredMethod : bus.getClass().getDeclaredMethods()) {
            if (!declaredMethod.isAnnotationPresent(BusHandler.class)) {
                continue;
            }

            if (declaredMethod.getParameterCount() != 1) {
                throw new IllegalStateException("BusHandler methods must have exactly one parameter!");
            }

            Class<?> parameter = declaredMethod.getParameterTypes()[0];

            try {
                handlers.put(parameter, publicLookup.unreflect(declaredMethod));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create method handle for " + declaredMethod, e);
            }
        }
    }

    public void handlePacket(Packet packet) throws Throwable {
        MethodHandle method = handlers.get(packet.getClass());

        if (method == null) {
            return;
        }

        method.invoke(bus, packet);
    }
}
