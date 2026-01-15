/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.api.event.SoulFireInstanceEvent;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.bot.*;
import com.soulfiremc.server.script.ScriptHelper;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class ScriptEventAPI {
  private final Context context;
  private final Map<String, List<EventListener>> eventListeners = new ConcurrentHashMap<>();

  @HostAccess.Export
  public void on(String event, Value callback) {
    eventListeners.computeIfAbsent(event, _ -> new CopyOnWriteArrayList<>())
      .add(new EventListener(false, callback));
  }

  @HostAccess.Export
  public void once(String event, Value callback) {
    eventListeners.computeIfAbsent(event, _ -> new CopyOnWriteArrayList<>())
      .add(new EventListener(true, callback));
  }

  public void forwardEvent(SoulFireInstanceEvent event) {
    if (event instanceof SoulFireBotEvent botEvent) {
      var botApi = new ScriptBotAPI(botEvent.connection());
      switch (event) {
        case BotConnectionInitEvent ignored -> forwardEvent("connectionInit", botApi);
        case BotPostEntityTickEvent ignored -> forwardEvent("postEntityTick", botApi);
        case BotPostTickEvent ignored -> forwardEvent("postTick", botApi);
        case BotPreEntityTickEvent ignored -> forwardEvent("preEntityTick", botApi);
        case BotPreTickEvent ignored -> forwardEvent("preTick", botApi);
        case ChatMessageReceiveEvent chatMessageReceiveEvent -> forwardEvent("message", botApi, ScriptHelper.componentToValue(context, chatMessageReceiveEvent.message()), chatMessageReceiveEvent.timestamp());
        case PreBotConnectEvent ignored -> forwardEvent("preConnect", botApi);
        default -> {
        }
      }
    } else {
      switch (event) {
        case AttackBotRemoveEvent botRemoveEvent -> forwardEvent("botRemove", new ScriptBotAPI(botRemoveEvent.botConnection()));
        case AttackEndedEvent ignored -> forwardEvent("attackEnded");
        case AttackStartEvent ignored -> forwardEvent("attackStart");
        case AttackTickEvent ignored -> forwardEvent("attackTick");
        default -> {
        }
      }
    }
  }

  public void forwardEvent(String event, Object... eventArgs) {
    var listeners = eventListeners.get(event);
    if (listeners != null) {
      listeners.removeIf(listener -> {
        listener.callback.executeVoid(eventArgs);
        return listener.once;
      });
    }
  }

  private record EventListener(boolean once, Value callback) {
  }
}
