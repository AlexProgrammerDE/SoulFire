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
package net.pistonmaster.serverwrecker.api;

import net.kyori.event.EventBus;
import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServerWreckerAPI {
    private static final EventBus<ServerWreckerEvent> eventBus = EventBus.create(ServerWreckerEvent.class);
    private static final List<Addon> addons = new ArrayList<>();
    private static ServerWrecker serverWrecker;

    /**
     * Get the current ServerWrecker instance for access to internals.
     *
     * @return The current ServerWrecker instance.
     */
    public static ServerWrecker getServerWrecker() {
        Objects.requireNonNull(serverWrecker, "ServerWreckerAPI not initialized! (Wait for ServerWreckerEnableEvent to fire)");
        return serverWrecker;
    }

    /**
     * Internal method to set the current ServerWrecker instance.
     *
     * @param serverWrecker The current ServerWrecker instance.
     */
    public static void setServerWrecker(ServerWrecker serverWrecker) {
        if (ServerWreckerAPI.serverWrecker != null) {
            throw new IllegalStateException("ServerWreckerAPI already initialized!");
        }

        ServerWreckerAPI.serverWrecker = serverWrecker;
    }

    public static void postEvent(ServerWreckerEvent event) {
        eventBus.post(event);
    }

    public static <T extends ServerWreckerEvent> void registerListener(Class<T> clazz, EventSubscriber<? super T> subscriber) {
        eventBus.subscribe(clazz, subscriber);
    }

    public static void registerListeners(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) {
                continue;
            }

            if (method.getParameterCount() != 1) {
                throw new IllegalArgumentException("Listener method must have exactly one parameter!");
            }

            if (!ServerWreckerEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Listener method parameter must be a subclass of ServerWreckerEvent!");
            }

            method.setAccessible(true);

            registerListener(method.getParameterTypes()[0].asSubclass(ServerWreckerEvent.class),
                    event -> {
                        try {
                            method.invoke(listener, event);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
        }
    }

    public static void unregisterListener(EventSubscriber<? extends ServerWreckerEvent> listener) {
        eventBus.unsubscribeIf(eventSubscriber -> eventSubscriber.equals(listener));
    }

    public static void registerAddon(Addon addon) {
        addons.add(addon);
        addon.onLoad();
    }

    public static List<Addon> getAddons() {
        return Collections.unmodifiableList(addons);
    }
}
