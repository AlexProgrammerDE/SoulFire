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
package com.soulfiremc.server.api;

import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireEvent;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds all instances of plugins running in this JVM.
 * The SoulFire server accesses this class on startup and advertises itself to all plugins.
 * It also holds the event manager for all events in this JVM.
 */
public class SoulFireAPI {
  private static final List<Plugin> SERVER_EXTENSIONS = new ArrayList<>();
  private static final LambdaManager EVENT_BUS =
    LambdaManager.threadSafe(new ASMGenerator())
      .setExceptionHandler(EventExceptionHandler.INSTANCE)
      .setEventFilter(
        (c, h) -> {
          if (SoulFireEvent.class.isAssignableFrom(c)) {
            return true;
          } else {
            throw new IllegalStateException("This event handler only accepts global events");
          }
        });

  private SoulFireAPI() {}

  public static void registerServerExtension(Plugin plugin) {
    SERVER_EXTENSIONS.add(plugin);
  }

  public static List<Plugin> getServerExtensions() {
    return Collections.unmodifiableList(SERVER_EXTENSIONS);
  }

  public static <E extends SoulFireEvent> void registerListener(Class<E> clazz, Consumer<E> consumer) {
    EVENT_BUS.registerConsumer(consumer, clazz);
  }

  public static <E extends SoulFireEvent> void unregisterListener(Class<E> clazz, Consumer<E> consumer) {
    EVENT_BUS.unregisterConsumer(consumer, clazz);
  }

  public static void registerListenersOfClass(Class<?> clazz) {
    EVENT_BUS.register(clazz);
  }

  public static void unregisterListenersOfClass(Class<?> clazz) {
    EVENT_BUS.unregister(clazz);
  }

  public static void registerListenersOfObject(Object object) {
    EVENT_BUS.register(object);
  }

  public static void unregisterListenersOfObject(Object object) {
    EVENT_BUS.unregister(object);
  }

  public static void postEvent(SoulFireEvent event) {
    EVENT_BUS.call(event);
  }

  public static LambdaManager getEventManager() {
    return EVENT_BUS;
  }
}
