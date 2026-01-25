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
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.bot.BotConnection;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Execution context for script nodes.
/// Provides access to the instance, bot (if BOT scope), variables, and node outputs.
/// This class is thread-safe for use in async node execution.
@Getter
public final class ScriptContext {
  private final InstanceManager instance;
  @Nullable
  private final BotConnection bot;
  private final ScriptScope scope;
  private final Map<String, Object> variables;
  private final Map<String, Map<String, Object>> nodeOutputs;
  private final ScriptEventListener eventListener;
  private volatile boolean cancelled;

  /// Creates a new script context for INSTANCE scope.
  ///
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for script execution events
  public ScriptContext(InstanceManager instance, ScriptEventListener eventListener) {
    this.instance = instance;
    this.bot = null;
    this.scope = ScriptScope.INSTANCE;
    this.variables = new ConcurrentHashMap<>();
    this.nodeOutputs = new ConcurrentHashMap<>();
    this.eventListener = eventListener;
  }

  /// Creates a new script context for BOT scope.
  ///
  /// @param instance      the SoulFire instance
  /// @param bot           the bot connection
  /// @param eventListener listener for script execution events
  public ScriptContext(InstanceManager instance, BotConnection bot, ScriptEventListener eventListener) {
    this.instance = instance;
    this.bot = bot;
    this.scope = ScriptScope.BOT;
    this.variables = new ConcurrentHashMap<>();
    this.nodeOutputs = new ConcurrentHashMap<>();
    this.eventListener = eventListener;
  }

  /// Gets the scheduler for async operations.
  /// Uses the bot scheduler if in BOT scope, otherwise the instance scheduler.
  ///
  /// @return the appropriate scheduler
  public SoulFireScheduler scheduler() {
    return bot != null ? bot.scheduler() : instance.scheduler();
  }

  /// Gets a variable value by name.
  ///
  /// @param name the variable name
  /// @return the variable value, or null if not set
  @Nullable
  public Object getVariable(String name) {
    return variables.get(name);
  }

  /// Gets a variable value with a default if not set.
  ///
  /// @param name         the variable name
  /// @param defaultValue the default value if not set
  /// @param <T>          the expected type
  /// @return the variable value or default
  @SuppressWarnings("unchecked")
  public <T> T getVariable(String name, T defaultValue) {
    var value = variables.get(name);
    return value != null ? (T) value : defaultValue;
  }

  /// Sets a variable value.
  ///
  /// @param name  the variable name
  /// @param value the value to set
  public void setVariable(String name, Object value) {
    variables.put(name, value);
    eventListener.onVariableChanged(name, value);
  }

  /// Stores the outputs of a node for later retrieval by connected nodes.
  ///
  /// @param nodeId  the node identifier
  /// @param outputs the output values
  public void storeNodeOutputs(String nodeId, Map<String, Object> outputs) {
    nodeOutputs.put(nodeId, new ConcurrentHashMap<>(outputs));
  }

  /// Gets the outputs from a previously executed node.
  ///
  /// @param nodeId the node identifier
  /// @return the outputs, or empty map if not found
  public Map<String, Object> getNodeOutputs(String nodeId) {
    return nodeOutputs.getOrDefault(nodeId, Map.of());
  }

  /// Gets a specific output value from a previously executed node.
  ///
  /// @param nodeId     the node identifier
  /// @param outputName the name of the output port
  /// @return the output value wrapped in Optional
  public Optional<Object> getNodeOutput(String nodeId, String outputName) {
    var outputs = nodeOutputs.get(nodeId);
    if (outputs == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(outputs.get(outputName));
  }

  /// Checks if execution has been cancelled.
  ///
  /// @return true if cancelled
  public boolean isCancelled() {
    return cancelled;
  }

  /// Cancels the script execution.
  public void cancel() {
    this.cancelled = true;
    eventListener.onScriptCancelled();
  }

  /// Requires the bot to be present (for BOT scope operations).
  ///
  /// @return the bot connection
  /// @throws IllegalStateException if not in BOT scope
  public BotConnection requireBot() {
    if (bot == null) {
      throw new IllegalStateException("This operation requires BOT scope but script is running in INSTANCE scope");
    }
    return bot;
  }

  /// The scope of script execution.
  public enum ScriptScope {
    /// Script runs at the instance level, affecting all bots.
    INSTANCE,
    /// Script runs for a specific bot.
    BOT
  }
}
