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
package com.soulfiremc.server.script;

import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.SoulFireEvent;
import com.soulfiremc.server.api.event.bot.BotConnectionInitEvent;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.api.event.bot.BotShouldRespawnEvent;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/// Service that manages event subscriptions for trigger nodes in scripts.
/// When a script is started, this service registers appropriate event listeners
/// for each trigger node and executes downstream nodes when events fire.
@Slf4j
public final class ScriptTriggerService {
  private final Map<UUID, List<Object>> scriptListeners = new ConcurrentHashMap<>();

  /// Registers event listeners for all trigger nodes in the script.
  ///
  /// @param scriptId the script ID
  /// @param graph    the script graph
  /// @param context  the execution context
  /// @param engine   the script engine
  public void registerTriggers(
    UUID scriptId,
    ScriptGraph graph,
    ScriptContext context,
    ScriptEngine engine
  ) {
    var listeners = new ArrayList<>();

    for (var node : graph.nodes().values()) {
      switch (node.type()) {
        case "trigger.on_tick" -> {
          var tickCount = new AtomicLong(0);
          Consumer<BotPreTickEvent> handler = event -> {
            if (context.isCancelled()) return;

            // Pass bot as input - scripts decide what to do with it
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("tickCount", NodeValue.ofNumber(tickCount.getAndIncrement()));
            engine.executeFromTrigger(graph, node.id(), context, inputs);
          };
          SoulFireAPI.registerListener(BotPreTickEvent.class, handler);
          listeners.add(new EventListenerHolder<>(BotPreTickEvent.class, handler));
          log.debug("Registered OnTick trigger for script {} node {}", scriptId, node.id());
        }

        case "trigger.on_chat" -> {
          Consumer<ChatMessageReceiveEvent> handler = event -> {
            if (context.isCancelled()) return;

            // Pass bot as input - scripts decide what to do with it
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("message", NodeValue.ofString(event.parseToPlainText())); // Raw message as plain text
            inputs.put("messagePlainText", NodeValue.ofString(event.parseToPlainText()));
            inputs.put("timestamp", NodeValue.ofNumber(event.timestamp()));
            engine.executeFromTrigger(graph, node.id(), context, inputs);
          };
          SoulFireAPI.registerListener(ChatMessageReceiveEvent.class, handler);
          listeners.add(new EventListenerHolder<>(ChatMessageReceiveEvent.class, handler));
          log.debug("Registered OnChat trigger for script {} node {}", scriptId, node.id());
        }

        case "trigger.on_death" -> {
          Consumer<BotShouldRespawnEvent> handler = event -> {
            if (context.isCancelled()) return;

            // Pass bot as input - scripts decide what to do with it
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("shouldRespawn", NodeValue.ofBoolean(event.shouldRespawn()));
            engine.executeFromTrigger(graph, node.id(), context, inputs);
          };
          SoulFireAPI.registerListener(BotShouldRespawnEvent.class, handler);
          listeners.add(new EventListenerHolder<>(BotShouldRespawnEvent.class, handler));
          log.debug("Registered OnDeath trigger for script {} node {}", scriptId, node.id());
        }

        case "trigger.on_join" -> {
          Consumer<BotConnectionInitEvent> handler = event -> {
            if (context.isCancelled()) return;

            // Pass bot as input - scripts decide what to do with it
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("botName", NodeValue.ofString(event.connection().accountName()));
            engine.executeFromTrigger(graph, node.id(), context, inputs);
          };
          SoulFireAPI.registerListener(BotConnectionInitEvent.class, handler);
          listeners.add(new EventListenerHolder<>(BotConnectionInitEvent.class, handler));
          log.debug("Registered OnJoin trigger for script {} node {}", scriptId, node.id());
        }

        case "trigger.on_interval" -> {
          var intervalMs = 1000L; // Default interval
          if (node.defaultInputs() != null) {
            var intervalValue = node.defaultInputs().get("intervalMs");
            if (intervalValue instanceof Number num) {
              intervalMs = num.longValue();
            }
          }

          var executionCount = new AtomicLong(0);
          var finalIntervalMs = intervalMs;
          var cancelled = new AtomicBoolean(false);

          Runnable task = new Runnable() {
            @Override
            public void run() {
              if (context.isCancelled() || cancelled.get()) return;

              // OnInterval doesn't have a specific bot context
              var inputs = new HashMap<String, NodeValue>();
              inputs.put("executionCount", NodeValue.ofNumber(executionCount.getAndIncrement()));
              engine.executeFromTrigger(graph, node.id(), context, inputs);

              // Reschedule if not cancelled
              if (!context.isCancelled() && !cancelled.get()) {
                context.scheduler().schedule(this, finalIntervalMs, TimeUnit.MILLISECONDS);
              }
            }
          };

          // Schedule first execution
          context.scheduler().schedule(task, intervalMs, TimeUnit.MILLISECONDS);
          listeners.add(new CancellableTask(cancelled));
          log.debug("Registered OnInterval trigger for script {} node {} with interval {}ms",
            scriptId, node.id(), intervalMs);
        }
      }
    }

    if (!listeners.isEmpty()) {
      scriptListeners.put(scriptId, listeners);
      log.info("Registered {} trigger(s) for script {}", listeners.size(), scriptId);
    }
  }

  /// Unregisters all event listeners for a script.
  ///
  /// @param scriptId the script ID
  public void unregisterTriggers(UUID scriptId) {
    var listeners = scriptListeners.remove(scriptId);
    if (listeners == null) {
      return;
    }

    for (var listener : listeners) {
      if (listener instanceof CancellableTask task) {
        task.cancel();
      } else if (listener instanceof EventListenerHolder<?> holder) {
        holder.unregister();
      }
    }

    log.info("Unregistered {} trigger(s) for script {}", listeners.size(), scriptId);
  }

  /// Checks if a script has active triggers.
  ///
  /// @param scriptId the script ID
  /// @return true if the script has registered triggers
  public boolean hasActiveTriggers(UUID scriptId) {
    return scriptListeners.containsKey(scriptId);
  }

  /// Holder for event listeners that enables proper unregistration.
  private record EventListenerHolder<E extends SoulFireEvent>(Class<E> eventClass, Consumer<E> consumer) {
    void unregister() {
      SoulFireAPI.unregisterListener(eventClass, consumer);
    }
  }

  /// Holder for cancellable interval tasks.
  private record CancellableTask(AtomicBoolean cancelled) {
    void cancel() {
      cancelled.set(true);
    }
  }
}
