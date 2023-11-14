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

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.event.EventExceptionHandler;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerGlobalEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerWreckerAPI {
    private static final LambdaManager eventBus = LambdaManager.basic(new ASMGenerator())
            .setExceptionHandler(EventExceptionHandler.INSTANCE)
            .setEventFilter((c, h) -> {
                if (ServerWreckerGlobalEvent.class.isAssignableFrom(c)) {
                    return true;
                } else {
                    throw new IllegalStateException("This event handler only accepts global events");
                }
            });
    private static final List<ServerExtension> SERVER_EXTENSIONS = new ArrayList<>();
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
        eventBus.call(event);
    }

    public static <T extends ServerWreckerGlobalEvent> void registerListener(Class<T> clazz, Consumer<? super T> subscriber) {
        eventBus.register(subscriber, clazz);
    }

    public static void registerListeners(Class<?> listenerClass) {
        eventBus.register(listenerClass);
    }

    public static void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }

    public static void registerServerExtension(ServerExtension serverExtension) {
        SERVER_EXTENSIONS.add(serverExtension);
        serverExtension.onLoad();
    }

    public static List<ServerExtension> getServerExtensions() {
        return Collections.unmodifiableList(SERVER_EXTENSIONS);
    }
}
