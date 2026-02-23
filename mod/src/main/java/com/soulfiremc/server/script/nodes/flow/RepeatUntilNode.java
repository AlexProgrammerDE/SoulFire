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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/// Flow control node that repeats execution until a condition is met.
/// Uses do-while semantics: the loop body always executes at least once.
/// After each iteration, the exec_check branch is fired. A ResultNode
/// at the end of the check chain calls setCheckResult to signal whether
/// the condition is met. Data-only nodes on the check chain are
/// re-evaluated each iteration via resetDataNodeTriggers.
public final class RepeatUntilNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.repeat_until")
    .displayName("Repeat Until")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("maxIterations", "Max Iterations", PortType.NUMBER, "10000", "Safety limit")
    )
    .addOutputs(
      PortDefinition.output(StandardPorts.EXEC_LOOP, "Loop", PortType.EXEC, "Executes for each iteration"),
      PortDefinition.output(StandardPorts.EXEC_CHECK, "Check", PortType.EXEC,
        "Evaluates condition after each iteration (must end with a Result node)"),
      PortDefinition.output(StandardPorts.EXEC_DONE, "Done", PortType.EXEC,
        "Executes when condition is met or limit reached"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current iteration index"),
      PortDefinition.output("conditionMet", "Condition Met", PortType.BOOLEAN,
        "Whether the condition was met (false if limit reached)")
    )
    .description("Repeats execution until a condition is met (do-while semantics)")
    .icon("repeat")
    .color("#607D8B")
    .addKeywords("loop", "repeat", "until", "while", "condition", "do-while")
    .supportsMuting(false)
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var maxIterations = getIntInput(inputs, "maxIterations", 10000);
    var index = new AtomicInteger(0);

    return Mono.defer(() -> {
        var i = index.getAndIncrement();
        if (i >= maxIterations) {
          return Mono.just(true);
        }

        var iterOutputs = Map.of(
          "index", NodeValue.ofNumber(i),
          "conditionMet", NodeValue.ofBoolean(false)
        );

        return runtime.executeDownstream(StandardPorts.EXEC_LOOP, iterOutputs)
          .then(Mono.fromRunnable(runtime::resetDataNodeTriggers))
          .then(runtime.executeDownstream(StandardPorts.EXEC_CHECK, iterOutputs))
          .then(Mono.fromSupplier(() -> {
            var wasSet = runtime.wasCheckResultSet();
            var conditionMet = runtime.getAndResetCheckResult();
            if (!wasSet) {
              // No ResultNode in check chain, break loop to avoid infinite iteration
              return true;
            }
            return conditionMet;
          }));
      })
      .repeat()
      .takeUntil(conditionMet -> conditionMet)
      .last(false)
      .flatMap(conditionMet -> {
        var finalIndex = index.get();
        var met = conditionMet && finalIndex <= maxIterations;
        return runtime.executeDownstream(StandardPorts.EXEC_DONE, Map.of(
            "index", NodeValue.ofNumber(finalIndex),
            "conditionMet", NodeValue.ofBoolean(met)
          ))
          .thenReturn(results("index", finalIndex, "conditionMet", met));
      });
  }
}
