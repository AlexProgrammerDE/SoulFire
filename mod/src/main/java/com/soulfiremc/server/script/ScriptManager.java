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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.bot.BotConnection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.SessionFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/// Manages script lifecycle including loading, saving, execution, and event tracking.
/// Each InstanceManager has its own ScriptManager for instance-scoped scripts.
@Slf4j
@RequiredArgsConstructor
public final class ScriptManager {
  private final InstanceManager instanceManager;
  private final SessionFactory sessionFactory;
  private final ScriptEngine engine;

  /// Map of script ID to loaded script graph.
  private final Map<UUID, ScriptGraph> loadedScripts = new ConcurrentHashMap<>();

  /// Map of script ID to running execution contexts (for instance scope).
  private final Map<UUID, RunningScript> runningInstanceScripts = new ConcurrentHashMap<>();

  /// Map of bot ID -> script ID -> running execution (for bot scope).
  private final Map<UUID, Map<UUID, RunningScript>> runningBotScripts = new ConcurrentHashMap<>();

  /// Global event listeners for all script executions.
  private final List<ScriptEventListener> globalListeners = new CopyOnWriteArrayList<>();

  /// Creates a new ScriptManager with a new ScriptEngine.
  public ScriptManager(InstanceManager instanceManager, SessionFactory sessionFactory) {
    this(instanceManager, sessionFactory, new ScriptEngine());
  }

  /// Registers a global event listener for all script executions.
  ///
  /// @param listener the listener to register
  public void addGlobalListener(ScriptEventListener listener) {
    globalListeners.add(listener);
  }

  /// Removes a global event listener.
  ///
  /// @param listener the listener to remove
  public void removeGlobalListener(ScriptEventListener listener) {
    globalListeners.remove(listener);
  }

  /// Loads a script graph and stores it for execution.
  ///
  /// @param scriptId the unique script identifier
  /// @param graph    the script graph to load
  public void loadScript(UUID scriptId, ScriptGraph graph) {
    loadedScripts.put(scriptId, graph);
    log.info("Loaded script: {} ({})", graph.scriptName(), scriptId);
  }

  /// Unloads a script and stops any running executions.
  ///
  /// @param scriptId the script to unload
  public void unloadScript(UUID scriptId) {
    loadedScripts.remove(scriptId);
    stopScript(scriptId);
    log.info("Unloaded script: {}", scriptId);
  }

  /// Gets a loaded script by ID.
  ///
  /// @param scriptId the script identifier
  /// @return the script graph, or null if not loaded
  @Nullable
  public ScriptGraph getScript(UUID scriptId) {
    return loadedScripts.get(scriptId);
  }

  /// Gets all loaded script IDs.
  ///
  /// @return set of loaded script IDs
  public Set<UUID> getLoadedScriptIds() {
    return Set.copyOf(loadedScripts.keySet());
  }

  /// Starts execution of a script in INSTANCE scope.
  ///
  /// @param scriptId the script to execute
  /// @return a future that completes when the script finishes, or empty if script not found
  public Optional<CompletableFuture<Void>> startInstanceScript(UUID scriptId) {
    var graph = loadedScripts.get(scriptId);
    if (graph == null) {
      log.warn("Script not found: {}", scriptId);
      return Optional.empty();
    }

    // Stop existing execution if running
    stopInstanceScript(scriptId);

    var compositeListener = new CompositeEventListener(scriptId, null, globalListeners);
    var future = engine.execute(graph, instanceManager, compositeListener);

    var runningScript = new RunningScript(scriptId, null, future, compositeListener);
    runningInstanceScripts.put(scriptId, runningScript);

    future.whenComplete((_, _) -> runningInstanceScripts.remove(scriptId));

    log.info("Started instance script: {} ({})", graph.scriptName(), scriptId);
    return Optional.of(future);
  }

  /// Starts execution of a script in BOT scope.
  ///
  /// @param scriptId the script to execute
  /// @param bot      the bot to run the script for
  /// @return a future that completes when the script finishes, or empty if script not found
  public Optional<CompletableFuture<Void>> startBotScript(UUID scriptId, BotConnection bot) {
    var graph = loadedScripts.get(scriptId);
    if (graph == null) {
      log.warn("Script not found: {}", scriptId);
      return Optional.empty();
    }

    var botId = bot.accountProfileId();

    // Stop existing execution if running
    stopBotScript(scriptId, botId);

    var compositeListener = new CompositeEventListener(scriptId, botId, globalListeners);
    var future = engine.execute(graph, instanceManager, bot, compositeListener);

    var botScripts = runningBotScripts.computeIfAbsent(botId, _ -> new ConcurrentHashMap<>());
    var runningScript = new RunningScript(scriptId, botId, future, compositeListener);
    botScripts.put(scriptId, runningScript);

    future.whenComplete((_, _) -> {
      var scripts = runningBotScripts.get(botId);
      if (scripts != null) {
        scripts.remove(scriptId);
        if (scripts.isEmpty()) {
          runningBotScripts.remove(botId);
        }
      }
    });

    log.info("Started bot script: {} ({}) for bot {}", graph.scriptName(), scriptId, bot.accountName());
    return Optional.of(future);
  }

  /// Stops a running instance script.
  ///
  /// @param scriptId the script to stop
  public void stopInstanceScript(UUID scriptId) {
    var running = runningInstanceScripts.remove(scriptId);
    if (running != null) {
      running.cancel();
      log.info("Stopped instance script: {}", scriptId);
    }
  }

  /// Stops a running bot script.
  ///
  /// @param scriptId the script to stop
  /// @param botId    the bot ID
  public void stopBotScript(UUID scriptId, UUID botId) {
    var botScripts = runningBotScripts.get(botId);
    if (botScripts != null) {
      var running = botScripts.remove(scriptId);
      if (running != null) {
        running.cancel();
        log.info("Stopped bot script: {} for bot {}", scriptId, botId);
      }
      if (botScripts.isEmpty()) {
        runningBotScripts.remove(botId);
      }
    }
  }

  /// Stops all running executions of a script (both instance and bot scope).
  ///
  /// @param scriptId the script to stop
  public void stopScript(UUID scriptId) {
    stopInstanceScript(scriptId);

    for (var botScripts : runningBotScripts.values()) {
      var running = botScripts.remove(scriptId);
      if (running != null) {
        running.cancel();
      }
    }
  }

  /// Stops all running scripts for a specific bot.
  ///
  /// @param botId the bot ID
  public void stopAllBotScripts(UUID botId) {
    var botScripts = runningBotScripts.remove(botId);
    if (botScripts != null) {
      for (var running : botScripts.values()) {
        running.cancel();
      }
      log.info("Stopped all scripts for bot: {}", botId);
    }
  }

  /// Stops all running scripts.
  public void stopAllScripts() {
    for (var running : runningInstanceScripts.values()) {
      running.cancel();
    }
    runningInstanceScripts.clear();

    for (var botScripts : runningBotScripts.values()) {
      for (var running : botScripts.values()) {
        running.cancel();
      }
    }
    runningBotScripts.clear();

    log.info("Stopped all running scripts");
  }

  /// Checks if a script is running in instance scope.
  ///
  /// @param scriptId the script ID
  /// @return true if running
  public boolean isInstanceScriptRunning(UUID scriptId) {
    return runningInstanceScripts.containsKey(scriptId);
  }

  /// Checks if a script is running for a specific bot.
  ///
  /// @param scriptId the script ID
  /// @param botId    the bot ID
  /// @return true if running
  public boolean isBotScriptRunning(UUID scriptId, UUID botId) {
    var botScripts = runningBotScripts.get(botId);
    return botScripts != null && botScripts.containsKey(scriptId);
  }

  /// Gets all running instance script IDs.
  ///
  /// @return set of running script IDs
  public Set<UUID> getRunningInstanceScripts() {
    return Set.copyOf(runningInstanceScripts.keySet());
  }

  /// Gets all running bot script IDs for a specific bot.
  ///
  /// @param botId the bot ID
  /// @return set of running script IDs
  public Set<UUID> getRunningBotScripts(UUID botId) {
    var botScripts = runningBotScripts.get(botId);
    return botScripts != null ? Set.copyOf(botScripts.keySet()) : Set.of();
  }

  /// Gets the script engine used by this manager.
  ///
  /// @return the script engine
  public ScriptEngine getEngine() {
    return engine;
  }

  /// Represents a running script execution.
  @Getter
  private static final class RunningScript {
    private final UUID scriptId;
    @Nullable
    private final UUID botId;
    private final CompletableFuture<Void> future;
    private final CompositeEventListener listener;

    RunningScript(
      UUID scriptId,
      @Nullable UUID botId,
      CompletableFuture<Void> future,
      CompositeEventListener listener
    ) {
      this.scriptId = scriptId;
      this.botId = botId;
      this.future = future;
      this.listener = listener;
    }

    void cancel() {
      future.cancel(true);
      listener.onScriptCancelled();
    }
  }

  /// Composite listener that delegates to multiple listeners.
  private record CompositeEventListener(
    UUID scriptId,
    @Nullable UUID botId,
    List<ScriptEventListener> delegates
  ) implements ScriptEventListener {

    @Override
    public void onNodeStarted(String nodeId) {
      for (var delegate : delegates) {
        try {
          delegate.onNodeStarted(nodeId);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }

    @Override
    public void onNodeCompleted(String nodeId, Map<String, Object> outputs) {
      for (var delegate : delegates) {
        try {
          delegate.onNodeCompleted(nodeId, outputs);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }

    @Override
    public void onNodeError(String nodeId, String error) {
      for (var delegate : delegates) {
        try {
          delegate.onNodeError(nodeId, error);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }

    @Override
    public void onScriptCompleted(boolean success) {
      for (var delegate : delegates) {
        try {
          delegate.onScriptCompleted(success);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }

    @Override
    public void onScriptCancelled() {
      for (var delegate : delegates) {
        try {
          delegate.onScriptCancelled();
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }

    @Override
    public void onVariableChanged(String name, Object value) {
      for (var delegate : delegates) {
        try {
          delegate.onVariableChanged(name, value);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
    }
  }
}
