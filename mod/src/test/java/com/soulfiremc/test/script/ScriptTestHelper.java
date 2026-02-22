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
package com.soulfiremc.test.script;

import com.google.gson.JsonElement;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import net.lenni0451.reflect.Fields;
import net.lenni0451.reflect.Objects;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Shared test utilities for script engine tests.
final class ScriptTestHelper {

  /// Minimal NodeRuntime for tests that need stateStore() (e.g., RateLimitNode, DebounceNode).
  static final NodeRuntime TEST_RUNTIME = new NodeRuntime() {
    private final ScriptStateStore stateStore = new ScriptStateStore();

    @Override
    public ScriptStateStore stateStore() {
      return stateStore;
    }

    @Override
    public InstanceManager instance() {
      return null;
    }

    @Override
    public SoulFireScheduler scheduler() {
      return null;
    }

    @Override
    public void log(String level, String message) {}
  };

  private ScriptTestHelper() {}

  /// Executes a node by type with given inputs and returns the output map.
  static Map<String, NodeValue> executeNode(String type, Map<String, NodeValue> inputs) {
    return NodeRegistry.create(type).executeReactive(null, inputs).block();
  }

  /// Executes a node by type with a specific runtime and inputs.
  static Map<String, NodeValue> executeNode(String type, NodeRuntime runtime, Map<String, NodeValue> inputs) {
    return NodeRegistry.create(type).executeReactive(runtime, inputs).block();
  }

  /// Runs a graph from a trigger node and returns the recording listener.
  static RecordingEventListener runGraph(ScriptGraph graph, String triggerId) {
    return runGraph(graph, triggerId, Map.of());
  }

  /// Runs a graph from a trigger node with event inputs and returns the recording listener.
  static RecordingEventListener runGraph(ScriptGraph graph, String triggerId, Map<String, NodeValue> eventInputs) {
    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, triggerId, context, eventInputs).block();
    return listener;
  }

  /// Asserts that no errors were recorded during graph execution.
  static void assertNoErrors(RecordingEventListener listener) {
    assertTrue(listener.errorNodes.isEmpty(), "No errors expected, got: " + listener.errorNodes);
  }

  /// Counts how many times a node was started during execution.
  static long countNodeExecutions(RecordingEventListener listener, String nodeId) {
    return listener.startedNodes.stream().filter(nodeId::equals).count();
  }

  /// Recording event listener for integration tests.
  static class RecordingEventListener implements ScriptEventListener {
    final List<String> startedNodes = new ArrayList<>();
    final List<String> completedNodes = new ArrayList<>();
    final Map<String, Map<String, NodeValue>> nodeOutputs = new HashMap<>();
    final Map<String, String> errorNodes = new HashMap<>();
    boolean scriptCompleted;

    @Override
    public void onNodeStarted(String nodeId) {
      startedNodes.add(nodeId);
    }

    @Override
    public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs) {
      completedNodes.add(nodeId);
      nodeOutputs.put(nodeId, outputs);
    }

    @Override
    public void onNodeError(String nodeId, String error) {
      errorNodes.put(nodeId, error);
    }

    @Override
    public void onScriptCompleted(boolean success) {
      scriptCompleted = success;
    }

    @Override
    public void onScriptCancelled() {}
  }

  /// Mock NodeRuntime for self-driving node tests (RepeatUntil, etc.).
  /// Tracks loop count, done-fired state, and check result.
  /// Use {@link #onDownstream} to customize executeDownstream behavior.
  static class MockNodeRuntime implements NodeRuntime {
    private final ScriptStateStore stateStore = new ScriptStateStore();
    private final AtomicBoolean checkResult = new AtomicBoolean(false);
    private final AtomicInteger loopCount = new AtomicInteger(0);
    private final AtomicBoolean doneFired = new AtomicBoolean(false);
    private BiConsumer<String, Map<String, NodeValue>> downstreamHandler = (_, _) -> {};

    /// Sets a custom handler for executeDownstream calls.
    /// The handler receives the handle name and the outputs map.
    MockNodeRuntime onDownstream(BiConsumer<String, Map<String, NodeValue>> handler) {
      this.downstreamHandler = handler;
      return this;
    }

    @Override
    public ScriptStateStore stateStore() {
      return stateStore;
    }

    @Override
    public InstanceManager instance() {
      return null;
    }

    @Override
    public SoulFireScheduler scheduler() {
      return null;
    }

    @Override
    public void log(String level, String message) {}

    @Override
    public Mono<Void> executeDownstream(String handle, Map<String, NodeValue> outputs) {
      if (StandardPorts.EXEC_LOOP.equals(handle)) {
        loopCount.incrementAndGet();
      }
      if (StandardPorts.EXEC_DONE.equals(handle)) {
        doneFired.set(true);
      }
      downstreamHandler.accept(handle, outputs);
      return Mono.empty();
    }

    @Override
    public void setCheckResult(boolean value) {
      checkResult.set(value);
    }

    @Override
    public boolean getAndResetCheckResult() {
      var v = checkResult.get();
      checkResult.set(false);
      return v;
    }

    @Override
    public void resetDataNodeTriggers() {}

    int loopCount() {
      return loopCount.get();
    }

    boolean doneFired() {
      return doneFired.get();
    }
  }

  /// Creates a mock BotConnection with working metadata and persistentMetadata holders.
  /// Uses lenni0451 Reflect to bypass the complex constructor.
  static BotConnection createMockBot() {
    var bot = Objects.allocate(BotConnection.class);
    Fields.setObject(bot, Fields.getDeclaredField(BotConnection.class, "metadata"), new MetadataHolder<>());
    Fields.setObject(bot, Fields.getDeclaredField(BotConnection.class, "persistentMetadata"), new MetadataHolder<JsonElement>());
    return bot;
  }

  /// Creates a mock InstanceManager with working metadata and persistentMetadata holders.
  /// Uses lenni0451 Reflect to bypass the complex constructor.
  static InstanceManager createMockInstance() {
    var instance = Objects.allocate(InstanceManager.class);
    Fields.setObject(instance, Fields.getDeclaredField(InstanceManager.class, "metadata"), new MetadataHolder<>());
    Fields.setObject(instance, Fields.getDeclaredField(InstanceManager.class, "persistentMetadata"), new MetadataHolder<JsonElement>());
    return instance;
  }

  /// Creates a NodeRuntime backed by a mock InstanceManager.
  static NodeRuntime createRuntimeWithInstance() {
    var mockInstance = createMockInstance();
    return new NodeRuntime() {
      @Override
      public ScriptStateStore stateStore() {
        return new ScriptStateStore();
      }

      @Override
      public InstanceManager instance() {
        return mockInstance;
      }

      @Override
      public SoulFireScheduler scheduler() {
        return null;
      }

      @Override
      public void log(String level, String message) {}
    };
  }
}
