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
package com.soulfiremc.server.script.nodes.state;

import com.google.gson.JsonParser;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.script.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/// State node that provides a simple state machine for bot behavior management.
public final class StateNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("state.state_machine")
    .displayName("State Machine")
    .category(CategoryRegistry.STATE)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("stateId", "State ID", PortType.STRING, "\"main\"", "State machine identifier"),
      PortDefinition.inputWithDefault("operation", "Operation", PortType.STRING, "\"get\"", "Operation: get, set, transition"),
      PortDefinition.inputWithDefault("newState", "New State", PortType.STRING, "\"\"", "New state (for set/transition)"),
      PortDefinition.inputWithDefault("allowedTransitions", "Allowed Transitions", PortType.STRING, "\"{}\"", "JSON map of allowed transitions")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("currentState", "Current State", PortType.STRING, "Current state value"),
      PortDefinition.output("previousState", "Previous State", PortType.STRING, "Previous state (after transition)"),
      PortDefinition.output("transitionAllowed", "Transition Allowed", PortType.BOOLEAN, "Whether transition was allowed")
    )
    .description("Simple state machine for bot behavior management")
    .icon("workflow")
    .color("#0EA5E9")
    .addKeywords("state", "machine", "fsm", "behavior", "mode", "transition")
    .build();

  // Use ConcurrentHashMap to store state machines per bot
  private static final MetadataKey<ConcurrentHashMap<String, StateData>> STATE_MACHINES_KEY =
    MetadataKey.of("script_state", "machines", ConcurrentHashMap.class);

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var stateId = getStringInput(inputs, "stateId", "main");
    var operation = getStringInput(inputs, "operation", "get").toLowerCase();
    var newState = getStringInput(inputs, "newState", "");
    var allowedTransitionsJson = getStringInput(inputs, "allowedTransitions", "{}");

    var stateMachines = bot.metadata().getOrSet(STATE_MACHINES_KEY, ConcurrentHashMap::new);
    var stateData = stateMachines.computeIfAbsent(stateId, _ -> new StateData("initial", ""));

    return switch (operation) {
      case "get" -> completed(results(
        "currentState", stateData.current,
        "previousState", stateData.previous,
        "transitionAllowed", true
      ));
      case "set" -> {
        var newData = new StateData(newState, stateData.current);
        stateMachines.put(stateId, newData);
        yield completed(results(
          "currentState", newState,
          "previousState", stateData.current,
          "transitionAllowed", true
        ));
      }
      case "transition" -> {
        var allowed = isTransitionAllowed(stateData.current, newState, allowedTransitionsJson);
        if (allowed) {
          var newData = new StateData(newState, stateData.current);
          stateMachines.put(stateId, newData);
        }
        yield completed(results(
          "currentState", allowed ? newState : stateData.current,
          "previousState", stateData.current,
          "transitionAllowed", allowed
        ));
      }
      default -> completed(results(
        "currentState", stateData.current,
        "previousState", stateData.previous,
        "transitionAllowed", false
      ));
    };
  }

  private boolean isTransitionAllowed(String current, String target, String allowedTransitionsJson) {
    if (allowedTransitionsJson.isEmpty() || "{}".equals(allowedTransitionsJson)) {
      return true; // No restrictions
    }

    try {
      var transitions = JsonParser.parseString(allowedTransitionsJson).getAsJsonObject();
      if (!transitions.has(current)) {
        return false; // No transitions defined from current state
      }

      var allowed = transitions.get(current);
      if (allowed.isJsonArray()) {
        Set<String> allowedSet = new HashSet<>();
        for (var elem : allowed.getAsJsonArray()) {
          allowedSet.add(elem.getAsString());
        }
        return allowedSet.contains(target);
      }
      return false;
    } catch (Exception _) {
      return true; // If parsing fails, allow all transitions
    }
  }

  public record StateData(String current, String previous) {}
}
