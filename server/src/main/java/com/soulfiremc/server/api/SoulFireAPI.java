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

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.SoulFireGlobalEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;

public class SoulFireAPI {
  private static final LambdaManager EVENT_BUS =
    LambdaManager.basic(new ASMGenerator())
      .setExceptionHandler(EventExceptionHandler.INSTANCE)
      .setEventFilter(
        (c, h) -> {
          if (SoulFireGlobalEvent.class.isAssignableFrom(c)) {
            return true;
          } else {
            throw new IllegalStateException("This event handler only accepts global events");
          }
        });
  private static final List<ServerPlugin> SERVER_EXTENSIONS = new ArrayList<>();
  private static SoulFireServer soulFireServer;

  private SoulFireAPI() {}

  /**
   * Get the current SoulFire instance for access to internals.
   *
   * @return The current SoulFire instance.
   */
  public static SoulFireServer getSoulFire() {
    Objects.requireNonNull(soulFireServer, "SoulFireAPI not initialized yet!");
    return soulFireServer;
  }

  /**
   * Internal method to set the current SoulFire instance.
   *
   * @param soulFireServer The current SoulFire instance.
   */
  public static void setSoulFire(SoulFireServer soulFireServer) {
    if (SoulFireAPI.soulFireServer != null) {
      throw new IllegalStateException("SoulFireAPI already initialized!");
    }

    SoulFireAPI.soulFireServer = soulFireServer;
  }

  public static void postEvent(SoulFireGlobalEvent event) {
    EVENT_BUS.call(event);
  }

  public static <T extends SoulFireGlobalEvent> void registerListener(
    Class<T> clazz, Consumer<? super T> subscriber) {
    EventUtil.runAndAssertChanged(EVENT_BUS, () -> EVENT_BUS.registerConsumer(subscriber, clazz));
  }

  public static void registerListeners(Class<?> listenerClass) {
    EventUtil.runAndAssertChanged(EVENT_BUS, () -> EVENT_BUS.register(listenerClass));
  }

  public static LambdaManager getEventBus() {
    return EVENT_BUS;
  }

  public static void registerServerExtension(ServerPlugin serverPlugin) {
    SERVER_EXTENSIONS.add(serverPlugin);
    serverPlugin.onLoad();
  }

  public static List<ServerPlugin> getServerExtensions() {
    return Collections.unmodifiableList(SERVER_EXTENSIONS);
  }
}
