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
import com.soulfiremc.server.api.event.bot.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/// Service that manages event subscriptions for trigger nodes in scripts.
/// When a script is started, this service registers appropriate event listeners
/// for each trigger node and executes downstream nodes when events fire.
///
/// Uses Reactor Sinks with backpressure to prevent overwhelming the system
/// when high-frequency events (like ticks) fire faster than they can be processed.
@Slf4j
public final class ScriptTriggerService {
  /// Buffer size for backpressure (events beyond this are dropped).
  private static final int TRIGGER_BUFFER_SIZE = 64;

  private final Map<UUID, List<Object>> scriptListeners = new ConcurrentHashMap<>();
  private final Map<String, Sinks.Many<Map<String, NodeValue>>> triggerSinks = new ConcurrentHashMap<>();
  private final Map<String, Disposable> triggerSubscriptions = new ConcurrentHashMap<>();

  /// Registers an event-based trigger with sink, subscription, and handler.
  private <E extends SoulFireEvent> void registerEventTrigger(
    UUID scriptId,
    String nodeId,
    ScriptGraph graph,
    ReactiveScriptContext context,
    ReactiveScriptEngine engine,
    Class<E> eventClass,
    Function<E, Map<String, NodeValue>> inputMapper,
    List<Object> listeners,
    String triggerName
  ) {
    var sinkKey = scriptId + ":" + nodeId;

    var sink = Sinks.many().multicast().<Map<String, NodeValue>>onBackpressureBuffer(TRIGGER_BUFFER_SIZE);
    triggerSinks.put(sinkKey, sink);

    var subscription = sink.asFlux()
      .flatMap(inputs -> engine.executeFromTrigger(graph, nodeId, context, inputs)
        .onErrorResume(e -> {
          log.error("Error in {} trigger execution", triggerName, e);
          return Mono.empty();
        }), 1)
      .subscribe();
    triggerSubscriptions.put(sinkKey, subscription);

    Consumer<E> handler = event -> {
      if (context.isCancelled()) {
        return;
      }
      var inputs = inputMapper.apply(event);
      if (inputs != null) {
        sink.tryEmitNext(inputs);
      }
    };
    SoulFireAPI.registerListener(eventClass, handler);
    listeners.add(new EventListenerHolder<>(eventClass, handler));
    log.debug("Registered {} trigger for script {} node {}", triggerName, scriptId, nodeId);
  }

  /// Registers event listeners for all trigger nodes in the script.
  /// Uses reactive sinks with backpressure for high-frequency triggers.
  ///
  /// @param scriptId the script ID
  /// @param graph    the script graph
  /// @param context  the reactive execution context
  /// @param engine   the reactive script engine
  public void registerTriggers(
    UUID scriptId,
    ScriptGraph graph,
    ReactiveScriptContext context,
    ReactiveScriptEngine engine
  ) {
    var listeners = new ArrayList<>();

    for (var node : graph.nodes().values()) {
      switch (node.type()) {
        case "trigger.on_tick" -> {
          var tickCount = new AtomicLong(0);
          registerEventTrigger(scriptId, node.id(), graph, context, engine,
            BotPreTickEvent.class, event -> {
              var inputs = new HashMap<String, NodeValue>();
              inputs.put("bot", NodeValue.ofBot(event.connection()));
              inputs.put("tickCount", NodeValue.ofNumber(tickCount.getAndIncrement()));
              return inputs;
            }, listeners, "OnTick");
        }

        case "trigger.on_chat" -> registerEventTrigger(scriptId, node.id(), graph, context, engine,
          ChatMessageReceiveEvent.class, event -> {
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("message", NodeValue.ofString(event.parseToPlainText()));
            inputs.put("messagePlainText", NodeValue.ofString(event.parseToPlainText()));
            inputs.put("timestamp", NodeValue.ofNumber(event.timestamp()));
            return inputs;
          }, listeners, "OnChat");

        case "trigger.on_death" -> registerEventTrigger(scriptId, node.id(), graph, context, engine,
          BotShouldRespawnEvent.class, event -> {
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("shouldRespawn", NodeValue.ofBoolean(event.shouldRespawn()));
            return inputs;
          }, listeners, "OnDeath");

        case "trigger.on_bot_init" -> registerEventTrigger(scriptId, node.id(), graph, context, engine,
          BotConnectionInitEvent.class, event -> {
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("botName", NodeValue.ofString(event.connection().accountName()));
            return inputs;
          }, listeners, "OnBotInit");

        case "trigger.on_join" -> {
          var triggeredBots = ConcurrentHashMap.<String>newKeySet();
          registerEventTrigger(scriptId, node.id(), graph, context, engine,
            BotPreTickEvent.class, event -> {
              var connection = event.connection();
              var botId = connection.accountName();
              if (connection.minecraft().player != null && triggeredBots.add(botId)) {
                var inputs = new HashMap<String, NodeValue>();
                inputs.put("bot", NodeValue.ofBot(connection));
                inputs.put("botName", NodeValue.ofString(connection.accountName()));
                return inputs;
              }
              return null;
            }, listeners, "OnJoin");
        }

        case "trigger.on_damage" -> registerEventTrigger(scriptId, node.id(), graph, context, engine,
          BotDamageEvent.class, event -> {
            var inputs = new HashMap<String, NodeValue>();
            inputs.put("bot", NodeValue.ofBot(event.connection()));
            inputs.put("amount", NodeValue.ofNumber(event.damageAmount()));
            inputs.put("previousHealth", NodeValue.ofNumber(event.previousHealth()));
            inputs.put("newHealth", NodeValue.ofNumber(event.newHealth()));
            return inputs;
          }, listeners, "OnDamage");

        case "trigger.on_interval" -> {
          var intervalMs = 1000L;
          if (node.defaultInputs() != null) {
            var intervalValue = node.defaultInputs().get("intervalMs");
            if (intervalValue instanceof Number num) {
              intervalMs = num.longValue();
            }
          }

          var executionCount = new AtomicLong(0);
          var finalIntervalMs = intervalMs;
          var cancelled = new AtomicBoolean(false);
          var sinkKey = scriptId + ":" + node.id();

          var sink = Sinks.many().multicast().<Map<String, NodeValue>>onBackpressureBuffer(TRIGGER_BUFFER_SIZE);
          triggerSinks.put(sinkKey, sink);

          var subscription = sink.asFlux()
            .flatMap(inputs -> engine.executeFromTrigger(graph, node.id(), context, inputs)
              .onErrorResume(e -> {
                log.error("Error in OnInterval trigger execution", e);
                return Mono.empty();
              }), 1)
            .subscribe();
          triggerSubscriptions.put(sinkKey, subscription);

          Runnable task = new Runnable() {
            @Override
            public void run() {
              if (context.isCancelled() || cancelled.get()) {
                return;
              }

              var inputs = new HashMap<String, NodeValue>();
              inputs.put("executionCount", NodeValue.ofNumber(executionCount.getAndIncrement()));

              sink.tryEmitNext(inputs);

              if (!context.isCancelled() && !cancelled.get()) {
                context.scheduler().schedule(this, finalIntervalMs, TimeUnit.MILLISECONDS);
              }
            }
          };

          context.scheduler().schedule(task, intervalMs, TimeUnit.MILLISECONDS);
          listeners.add(new CancellableTask(cancelled));
          log.debug("Registered OnInterval trigger for script {} node {} with interval {}ms",
            scriptId, node.id(), intervalMs);
        }

        case "trigger.on_script_init" -> {
          // Fire immediately when script starts
          var inputs = new HashMap<String, NodeValue>();
          inputs.put("timestamp", NodeValue.ofNumber(System.currentTimeMillis()));

          // Execute asynchronously to not block registration
          engine.executeFromTrigger(graph, node.id(), context, inputs)
            .onErrorResume(e -> {
              log.error("Error in OnScriptInit trigger execution", e);
              return Mono.empty();
            })
            .subscribe();
          log.debug("Registered and fired OnScriptInit trigger for script {} node {}", scriptId, node.id());
        }

        case "trigger.on_script_end" -> {
          // Store for later execution when script stops
          var endTrigger = new ScriptEndTrigger(graph, node.id(), context, engine);
          listeners.add(endTrigger);
          log.debug("Registered OnScriptEnd trigger for script {} node {}", scriptId, node.id());
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

    // First, fire any OnScriptEnd triggers synchronously
    for (var listener : listeners) {
      if (listener instanceof ScriptEndTrigger endTrigger) {
        endTrigger.fire();
      }
    }

    // Then clean up all listeners
    for (var listener : listeners) {
      if (listener instanceof CancellableTask task) {
        task.cancel();
      } else if (listener instanceof EventListenerHolder<?> holder) {
        holder.unregister();
      }
    }

    // Clean up sinks and subscriptions for this script
    var keysToRemove = triggerSinks.keySet().stream()
      .filter(key -> key.startsWith(scriptId + ":"))
      .toList();

    for (var key : keysToRemove) {
      var subscription = triggerSubscriptions.remove(key);
      if (subscription != null) {
        subscription.dispose();
      }
      var sink = triggerSinks.remove(key);
      if (sink != null) {
        sink.tryEmitComplete();
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

  /// Holder for script end triggers that fire when the script is stopped.
  private record ScriptEndTrigger(
    ScriptGraph graph,
    String nodeId,
    ReactiveScriptContext context,
    ReactiveScriptEngine engine
  ) {
    void fire() {
      try {
        var inputs = new HashMap<String, NodeValue>();
        inputs.put("timestamp", NodeValue.ofNumber(System.currentTimeMillis()));

        // Execute synchronously (block) to ensure cleanup completes before script fully stops
        engine.executeFromTrigger(graph, nodeId, context, inputs)
          .onErrorResume(e -> {
            log.error("Error in OnScriptEnd trigger execution", e);
            return Mono.empty();
          })
          .block(Duration.ofSeconds(5));
      } catch (Exception e) {
        log.error("Error firing OnScriptEnd trigger", e);
      }
    }
  }
}
