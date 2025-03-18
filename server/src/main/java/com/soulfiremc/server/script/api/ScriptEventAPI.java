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
package com.soulfiremc.server.script.api;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScriptEventAPI {
  private final Map<String, List<EventListener>> eventListeners = new ConcurrentHashMap<>();

  public ScriptEventAPI() {
  }

  @HostAccess.Export
  public void on(String event, Value callback) {
    eventListeners.computeIfAbsent(event, key -> new CopyOnWriteArrayList<>())
        .add(new EventListener(false, callback));
  }

  @HostAccess.Export
  public void once(String event, Value callback) {
    eventListeners.computeIfAbsent(event, key -> new CopyOnWriteArrayList<>())
        .add(new EventListener(true, callback));
  }

  public void forwardEvent(String event, Object... eventArgs) {
    var listeners = eventListeners.get(event);
    if (listeners != null) {
      listeners.forEach(listener -> {
        listener.callback.execute(eventArgs);
        if (listener.once) {
          listeners.remove(listener);
        }
      });
    }
  }

  private record EventListener(boolean once, Value callback) {
  }
}
