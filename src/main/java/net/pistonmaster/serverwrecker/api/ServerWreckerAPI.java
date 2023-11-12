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
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.event.GlobalEventHandler;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerGlobalEvent;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServerWreckerAPI {
    private static final EventBus<ServerWreckerGlobalEvent> eventBus = EventBus.create(ServerWreckerGlobalEvent.class);
    private static final List<Addon> addons = new ArrayList<>();
    private static ServerWreckerServer serverWreckerServer;

    private ServerWreckerAPI() {
    }

    /**
     * Get the current ServerWrecker instance for access to internals.
     *
     * @return The current ServerWrecker instance.
     */
    public static ServerWreckerServer getServerWrecker() {
        Objects.requireNonNull(serverWreckerServer, "ServerWreckerAPI not initialized! (Wait for ServerWreckerEnableEvent to fire)");
        return serverWreckerServer;
    }

    /**
     * Internal method to set the current ServerWrecker instance.
     *
     * @param serverWreckerServer The current ServerWrecker instance.
     */
    public static void setServerWrecker(ServerWreckerServer serverWreckerServer) {
        if (ServerWreckerAPI.serverWreckerServer != null) {
            throw new IllegalStateException("ServerWreckerAPI already initialized!");
        }

        ServerWreckerAPI.serverWreckerServer = serverWreckerServer;
    }

    public static void postEvent(ServerWreckerGlobalEvent event) {
        eventBus.post(event);
    }

    public static <T extends ServerWreckerGlobalEvent> void registerListener(Class<T> clazz, EventSubscriber<? super T> subscriber) {
        eventBus.subscribe(clazz, subscriber);
    }

    public static void registerListeners(Object listener) {
        var publicLookup = MethodHandles.publicLookup();
        for (var method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GlobalEventHandler.class)) {
                continue;
            }

            if (method.getParameterCount() != 1) {
                throw new IllegalArgumentException("Listener method must have exactly one parameter!");
            }

            if (!ServerWreckerGlobalEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Listener method parameter must be a subclass of ServerWreckerGlobalEvent!");
            }

            method.setAccessible(true);

            try {
                var methodHandle = publicLookup.unreflect(method);

                registerListener(method.getParameterTypes()[0].asSubclass(ServerWreckerGlobalEvent.class),
                        event -> {
                            try {
                                methodHandle.invoke(listener, event);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        });
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to create method handle!", e);
            }
        }
    }

    public static void unregisterListener(EventSubscriber<? extends ServerWreckerGlobalEvent> listener) {
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
