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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.SessionFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/// Manages script lifecycle including loading, activation, and event tracking.
/// Scripts are reactive state machines that activate (register listeners) and
/// deactivate (unregister + cleanup). There is no concept of scripts "running"
/// to completion - they simply listen for trigger events when active.
@Slf4j
@RequiredArgsConstructor
public final class ScriptManager {
  private final InstanceManager instanceManager;
  private final SessionFactory sessionFactory;
  private final ScriptEngine engine;
  private final ScriptTriggerService triggerService;

  /// Map of script ID to loaded script graph.
  private final Map<UUID, ScriptGraph> loadedScripts = new ConcurrentHashMap<>();

  /// Map of script ID to active script context.
  private final Map<UUID, ActiveScript> activeScripts = new ConcurrentHashMap<>();

  /// Global event listeners for all script executions.
  private final List<ScriptEventListener> globalListeners = new CopyOnWriteArrayList<>();

  /// Creates a new ScriptManager with a new ScriptEngine and TriggerService.
  public ScriptManager(InstanceManager instanceManager, SessionFactory sessionFactory) {
    this(instanceManager, sessionFactory, new ScriptEngine(), new ScriptTriggerService());
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

  /// Loads a script graph and stores it for activation.
  ///
  /// @param scriptId the unique script identifier
  /// @param graph    the script graph to load
  public void loadScript(UUID scriptId, ScriptGraph graph) {
    loadedScripts.put(scriptId, graph);
    log.info("Loaded script: {} ({})", graph.scriptName(), scriptId);
  }

  /// Unloads a script and deactivates it if active.
  ///
  /// @param scriptId the script to unload
  public void unloadScript(UUID scriptId) {
    loadedScripts.remove(scriptId);
    deactivateScript(scriptId);
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

  /// Activates a script by registering its trigger event listeners.
  /// The script will begin responding to trigger events (tick, chat, etc.).
  ///
  /// @param scriptId the script to activate
  /// @return true if activated successfully, false if script not found or already active
  public boolean activateScript(UUID scriptId) {
    return activateScript(scriptId, null);
  }

  /// Activates a script with a custom event listener.
  ///
  /// @param scriptId      the script to activate
  /// @param eventListener optional custom event listener (in addition to global listeners)
  /// @return true if activated successfully
  public boolean activateScript(UUID scriptId, @Nullable ScriptEventListener eventListener) {
    var graph = loadedScripts.get(scriptId);
    if (graph == null) {
      log.warn("Script not found: {}", scriptId);
      return false;
    }

    if (activeScripts.containsKey(scriptId)) {
      log.warn("Script already active: {}", scriptId);
      return false;
    }

    // Create composite listener
    var compositeListener = new CompositeEventListener(scriptId, globalListeners, eventListener);

    // Create context
    var context = new ScriptContext(instanceManager, compositeListener);

    // Register trigger listeners
    triggerService.registerTriggers(scriptId, graph, context, engine);

    // Store active script
    var activeScript = new ActiveScript(scriptId, graph, context, compositeListener);
    activeScripts.put(scriptId, activeScript);

    log.info("Activated script: {} ({})", graph.scriptName(), scriptId);
    return true;
  }

  /// Deactivates an active script.
  /// Unregisters all event listeners and cancels any pending async operations.
  ///
  /// @param scriptId the script to deactivate
  public void deactivateScript(UUID scriptId) {
    var activeScript = activeScripts.remove(scriptId);
    if (activeScript == null) {
      return;
    }

    // Unregister trigger listeners
    triggerService.unregisterTriggers(scriptId);

    // Cancel pending operations
    activeScript.context.cancel();

    log.info("Deactivated script: {}", scriptId);
  }

  /// Deactivates all active scripts.
  public void deactivateAllScripts() {
    for (var scriptId : Set.copyOf(activeScripts.keySet())) {
      deactivateScript(scriptId);
    }
    log.info("Deactivated all scripts");
  }

  /// Checks if a script is active.
  ///
  /// @param scriptId the script ID
  /// @return true if active
  public boolean isScriptActive(UUID scriptId) {
    return activeScripts.containsKey(scriptId);
  }

  /// Gets all active script IDs.
  ///
  /// @return set of active script IDs
  public Set<UUID> getActiveScripts() {
    return Set.copyOf(activeScripts.keySet());
  }

  /// Gets the context for an active script.
  ///
  /// @param scriptId the script ID
  /// @return the context, or null if not active
  @Nullable
  public ScriptContext getActiveScriptContext(UUID scriptId) {
    var activeScript = activeScripts.get(scriptId);
    return activeScript != null ? activeScript.context : null;
  }

  /// Gets the script engine used by this manager.
  ///
  /// @return the script engine
  public ScriptEngine getEngine() {
    return engine;
  }

  /// Gets the trigger service used by this manager.
  ///
  /// @return the trigger service
  public ScriptTriggerService getTriggerService() {
    return triggerService;
  }

  /// Represents an active script with its context.
  @Getter
  private static final class ActiveScript {
    private final UUID scriptId;
    private final ScriptGraph graph;
    private final ScriptContext context;
    private final CompositeEventListener listener;

    ActiveScript(UUID scriptId, ScriptGraph graph, ScriptContext context, CompositeEventListener listener) {
      this.scriptId = scriptId;
      this.graph = graph;
      this.context = context;
      this.listener = listener;
    }
  }

  /// Composite listener that delegates to multiple listeners.
  private static final class CompositeEventListener implements ScriptEventListener {
    private final UUID scriptId;
    private final List<ScriptEventListener> delegates;
    @Nullable
    private final ScriptEventListener customListener;

    CompositeEventListener(UUID scriptId, List<ScriptEventListener> delegates, @Nullable ScriptEventListener customListener) {
      this.scriptId = scriptId;
      this.delegates = delegates;
      this.customListener = customListener;
    }

    @Override
    public void onNodeStarted(String nodeId) {
      for (var delegate : delegates) {
        try {
          delegate.onNodeStarted(nodeId);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
      if (customListener != null) {
        try {
          customListener.onNodeStarted(nodeId);
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
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
      if (customListener != null) {
        try {
          customListener.onNodeCompleted(nodeId, outputs);
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
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
      if (customListener != null) {
        try {
          customListener.onNodeError(nodeId, error);
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
        }
      }
    }

    @Override
    public void onScriptCompleted(boolean success) {
      // Scripts don't "complete" in the activatable model, but we keep this for compatibility
      for (var delegate : delegates) {
        try {
          delegate.onScriptCompleted(success);
        } catch (Exception e) {
          log.error("Error in script event listener", e);
        }
      }
      if (customListener != null) {
        try {
          customListener.onScriptCompleted(success);
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
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
      if (customListener != null) {
        try {
          customListener.onScriptCancelled();
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
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
      if (customListener != null) {
        try {
          customListener.onVariableChanged(name, value);
        } catch (Exception e) {
          log.error("Error in custom event listener", e);
        }
      }
    }
  }
}
