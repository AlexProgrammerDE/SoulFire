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

import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptGraph;
import com.soulfiremc.server.script.StandardPorts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for RepeatUntilNode and ResultNode: mock-based unit tests and engine integration tests.
final class RepeatUntilTest {

  // ==================== Mock-Based Unit Tests ====================

  @Test
  void repeatUntilLoopsUntilConditionMet() {
    var runtime = new MockNodeRuntime();
    runtime.onDownstream((handle, _) -> {
      if (StandardPorts.EXEC_CHECK.equals(handle)) {
        runtime.setCheckResult(runtime.loopCount() >= 3);
      }
    });

    var result = executeNode("flow.repeat_until", runtime, Map.of(
      "maxIterations", NodeValue.ofNumber(10000)
    ));

    assertEquals(3, runtime.loopCount(), "Loop body should execute exactly 3 times");
    assertTrue(result.get("conditionMet").asBoolean(false), "Condition should be met");
    assertEquals(3, result.get("index").asInt(0), "Final index should be 3");
  }

  @Test
  void repeatUntilBodyExecutesAtLeastOnce() {
    var runtime = new MockNodeRuntime();
    runtime.onDownstream((handle, _) -> {
      if (StandardPorts.EXEC_CHECK.equals(handle)) {
        runtime.setCheckResult(true);
      }
    });

    var result = executeNode("flow.repeat_until", runtime, Map.of(
      "maxIterations", NodeValue.ofNumber(10000)
    ));

    assertEquals(1, runtime.loopCount(), "Loop body should execute at least once (do-while)");
    assertTrue(result.get("conditionMet").asBoolean(false), "Condition should be met");
  }

  @Test
  void repeatUntilRespectsMaxIterations() {
    var runtime = new MockNodeRuntime();
    // Always set checkResult to false to simulate condition never met
    runtime.onDownstream((handle, _) -> {
      if (StandardPorts.EXEC_CHECK.equals(handle)) {
        runtime.setCheckResult(false);
      }
    });

    var result = executeNode("flow.repeat_until", runtime, Map.of(
      "maxIterations", NodeValue.ofNumber(5)
    ));

    assertEquals(5, runtime.loopCount(), "Loop body should execute exactly maxIterations times");
    assertFalse(result.get("conditionMet").asBoolean(true),
      "conditionMet should be false when stopped by maxIterations");
  }

  @Test
  void repeatUntilWithNoCheckConnectionTerminatesAfterOne() {
    var runtime = new MockNodeRuntime();
    // Nothing connected to exec_check, wasCheckResultSet stays false -> breaks after 1 iteration

    var result = executeNode("flow.repeat_until", runtime, Map.of(
      "maxIterations", NodeValue.ofNumber(3)
    ));

    assertEquals(1, runtime.loopCount(), "Should terminate after 1 iteration when no ResultNode sets check result");
    assertTrue(result.get("conditionMet").asBoolean(false),
      "conditionMet should be true (empty check chain guard)");
  }

  @Test
  void repeatUntilExecDoneFires() {
    var runtime = new MockNodeRuntime();
    runtime.onDownstream((handle, _) -> {
      if (StandardPorts.EXEC_CHECK.equals(handle)) {
        runtime.setCheckResult(true);
      }
    });

    executeNode("flow.repeat_until", runtime, Map.of(
      "maxIterations", NodeValue.ofNumber(10000)
    ));

    assertTrue(runtime.doneFired(), "exec_done should fire after condition is met");
  }

  @Test
  void resultNodeSetsCheckResult() {
    var runtime = new MockNodeRuntime();

    executeNode("flow.result", runtime, Map.of("value", NodeValue.ofBoolean(true)));
    assertTrue(runtime.getAndResetCheckResult(), "ResultNode should set checkResult to true");

    executeNode("flow.result", runtime, Map.of("value", NodeValue.ofBoolean(false)));
    assertFalse(runtime.getAndResetCheckResult(), "ResultNode should set checkResult to false");
  }

  @Test
  void resultNodeDefaultsToFalse() {
    var runtime = new MockNodeRuntime();
    runtime.setCheckResult(true);

    executeNode("flow.result", runtime, Map.of());
    assertFalse(runtime.getAndResetCheckResult(), "ResultNode should default to false when no value input");
  }

  // ==================== Engine Integration Tests ====================

  @Test
  void repeatUntilEngineIntegrationWithResultNode() {
    // RepeatUntil with exec_check → ResultNode.
    // CompareNode (data-only) feeds ResultNode's value input.
    // compare: index >= 2 → loops at i=0,1, stops at i=2
    var graph = ScriptGraph.builder("test-repeat-until", "RepeatUntil Integration")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("compare", "logic.compare", Map.of("b", 2, "operator", ">="))
      .addNode("result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      .addExecutionEdge("repeat", "exec_check", "result", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .addDataEdge("repeat", "index", "compare", "a")
      .addDataEdge("compare", "result", "result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("repeat"),
      "RepeatUntil node should complete");
    assertTrue(listener.completedNodes.contains("done_print"),
      "Done print should fire");

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertTrue(repeatOutputs.get("conditionMet").asBoolean(false),
      "Condition should be met");
    assertEquals(3, repeatOutputs.get("index").asInt(-1),
      "Final index should be 3 after 3 iterations");

    assertEquals(3, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute 3 times");
  }

  @Test
  void repeatUntilEngineStopsAtMaxIterations() {
    // exec_check → ResultNode with value always false.
    var graph = ScriptGraph.builder("test-repeat-max", "RepeatUntil Max Iterations")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 4))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("false_const", "constant.boolean", Map.of("value", false))
      .addNode("result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      .addExecutionEdge("repeat", "exec_check", "result", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .addDataEdge("false_const", "value", "result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertFalse(repeatOutputs.get("conditionMet").asBoolean(true),
      "conditionMet should be false when stopped by maxIterations");
    assertEquals(5, repeatOutputs.get("index").asInt(-1),
      "Final index should be 5 (4 body executions + 1 maxIterations guard)");

    assertEquals(4, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute maxIterations times");
  }

  @Test
  void repeatUntilEngineDoWhileSemanticsFirstIterationTrue() {
    // exec_check → ResultNode with value always true.
    var graph = ScriptGraph.builder("test-repeat-dowhile", "RepeatUntil Do-While")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("true_const", "constant.boolean", Map.of("value", true))
      .addNode("result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      .addExecutionEdge("repeat", "exec_check", "result", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .addDataEdge("true_const", "value", "result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertTrue(repeatOutputs.get("conditionMet").asBoolean(false),
      "Condition should be met");

    assertEquals(1, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute exactly once (do-while)");
  }

  @Test
  void repeatUntilEngineWithNotNode() {
    // Compare(index < 3) → Not → Result.
    // i=0,1,2: true → Not → false → keep looping
    // i=3: false → Not → true → stop
    var graph = ScriptGraph.builder("test-repeat-not", "RepeatUntil With Not")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("compare", "logic.compare", Map.of("b", 3, "operator", "<"))
      .addNode("not", "logic.not", null)
      .addNode("result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      .addExecutionEdge("repeat", "exec_check", "result", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .addDataEdge("repeat", "index", "compare", "a")
      .addDataEdge("compare", "result", "not", "value")
      .addDataEdge("not", "result", "result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertTrue(repeatOutputs.get("conditionMet").asBoolean(false),
      "Condition should be met");
    assertEquals(4, repeatOutputs.get("index").asInt(-1),
      "Final index should be 4 (stopped when i=3 >= 3)");

    assertEquals(4, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute 4 times (i=0,1,2,3)");
  }

  @Test
  void repeatUntilEngineWithNoCheckConnection() {
    // No ResultNode on exec_check → wasCheckResultSet stays false → terminates after 1 iteration.
    var graph = ScriptGraph.builder("test-repeat-no-check", "RepeatUntil No Check")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertTrue(repeatOutputs.get("conditionMet").asBoolean(false),
      "conditionMet should be true (empty check chain guard)");

    assertEquals(1, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute exactly once when no ResultNode sets check result");
  }

  @Test
  void repeatUntilEmptyCheckChainTerminatesAfterOneIteration() {
    // exec_check has no downstream nodes at all (no ResultNode).
    // The loop should detect wasCheckResultSet==false and break after one iteration.
    var graph = ScriptGraph.builder("test-repeat-empty-check", "RepeatUntil Empty Check")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("loop_print", "action.print", Map.of("message", "looping"))
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_loop", "loop_print", "in")
      // exec_check is NOT connected to anything
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");

    assertEquals(1, countNodeExecutions(listener, "loop_print"),
      "Loop body should execute exactly once when check chain has no ResultNode");
  }

  @Test
  void repeatUntilEngineChainedDataOnlyNodesRefreshed() {
    // Chained data-only: const(1) → add → compare → result.
    // add computes index + 1, compare checks (index + 1) >= 3
    // i=0: 1>=3=false, i=1: 2>=3=false, i=2: 3>=3=true → stop
    var graph = ScriptGraph.builder("test-repeat-chained", "RepeatUntil Chained Data")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("repeat", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("const_one", "constant.number", Map.of("value", 1))
      .addNode("add", "math.add", null)
      .addNode("const_three", "constant.number", Map.of("value", 3))
      .addNode("compare", "logic.compare", Map.of("operator", ">="))
      .addNode("result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "repeat", "in")
      .addExecutionEdge("repeat", "exec_check", "result", "in")
      .addExecutionEdge("repeat", "exec_done", "done_print", "in")
      .addDataEdge("repeat", "index", "add", "a")
      .addDataEdge("const_one", "value", "add", "b")
      .addDataEdge("add", "result", "compare", "a")
      .addDataEdge("const_three", "value", "compare", "b")
      .addDataEdge("compare", "result", "result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var repeatOutputs = listener.nodeOutputs.get("repeat");
    assertNotNull(repeatOutputs, "RepeatUntil should have outputs");
    assertTrue(repeatOutputs.get("conditionMet").asBoolean(false),
      "Condition should be met");
    assertEquals(3, repeatOutputs.get("index").asInt(-1),
      "Should stop after 3 iterations (i=0,1,2)");
  }

  // ==================== Nested Loop Tests ====================

  @Test
  void nestedRepeatUntilEngineIntegration() {
    // Do-while semantics: compare(index >= N) → body runs N+1 times
    // Outer: index >= 2 → 3 body iterations (i=0,1,2), finalIndex=3
    // Inner: index >= 1 → 2 body iterations (j=0,1), finalIndex=2
    // Total inner body executions: 3 * 2 = 6
    var graph = ScriptGraph.builder("test-nested-repeat", "Nested RepeatUntil")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("outer", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner_body", "action.print", Map.of("message", "inner"))
      .addNode("inner_compare", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("inner_result", "flow.result", null)
      .addNode("outer_compare", "logic.compare", Map.of("b", 2, "operator", ">="))
      .addNode("outer_result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      // Execution edges
      .addExecutionEdge("trigger", "out", "outer", "in")
      .addExecutionEdge("outer", "exec_loop", "inner", "in")
      .addExecutionEdge("inner", "exec_loop", "inner_body", "in")
      .addExecutionEdge("inner", "exec_check", "inner_result", "in")
      .addExecutionEdge("outer", "exec_check", "outer_result", "in")
      .addExecutionEdge("outer", "exec_done", "done_print", "in")
      // Data edges: inner loop condition
      .addDataEdge("inner", "index", "inner_compare", "a")
      .addDataEdge("inner_compare", "result", "inner_result", "value")
      // Data edges: outer loop condition
      .addDataEdge("outer", "index", "outer_compare", "a")
      .addDataEdge("outer_compare", "result", "outer_result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("outer"),
      "Outer RepeatUntil should complete");
    assertTrue(listener.completedNodes.contains("done_print"),
      "Done print should fire");

    var outerOutputs = listener.nodeOutputs.get("outer");
    assertNotNull(outerOutputs, "Outer should have outputs");
    assertTrue(outerOutputs.get("conditionMet").asBoolean(false),
      "Outer condition should be met");
    assertEquals(3, outerOutputs.get("index").asInt(-1),
      "Outer final index should be 3");

    assertEquals(6, countNodeExecutions(listener, "inner_body"),
      "Inner body should execute 6 times total (3 outer * 2 inner)");
  }

  @Test
  void nestedRepeatUntilWithDifferentIterationCounts() {
    // Outer: index >= 1 → 2 body iterations
    // Inner: index >= 3 → 4 body iterations
    // Total inner body: 2 * 4 = 8
    var graph = ScriptGraph.builder("test-nested-asymmetric", "Nested Asymmetric")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("outer", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner_body", "action.print", Map.of("message", "inner"))
      .addNode("inner_compare", "logic.compare", Map.of("b", 3, "operator", ">="))
      .addNode("inner_result", "flow.result", null)
      .addNode("outer_compare", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("outer_result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "outer", "in")
      .addExecutionEdge("outer", "exec_loop", "inner", "in")
      .addExecutionEdge("inner", "exec_loop", "inner_body", "in")
      .addExecutionEdge("inner", "exec_check", "inner_result", "in")
      .addExecutionEdge("outer", "exec_check", "outer_result", "in")
      .addExecutionEdge("outer", "exec_done", "done_print", "in")
      .addDataEdge("inner", "index", "inner_compare", "a")
      .addDataEdge("inner_compare", "result", "inner_result", "value")
      .addDataEdge("outer", "index", "outer_compare", "a")
      .addDataEdge("outer_compare", "result", "outer_result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var outerOutputs = listener.nodeOutputs.get("outer");
    assertNotNull(outerOutputs, "Outer should have outputs");
    assertTrue(outerOutputs.get("conditionMet").asBoolean(false),
      "Outer condition should be met");

    assertEquals(8, countNodeExecutions(listener, "inner_body"),
      "Inner body should execute 8 times total (2 outer * 4 inner)");
  }

  @Test
  void tripleNestedRepeatUntil() {
    // Each level: index >= 1 → 2 body iterations
    // Total innermost body: 2 * 2 * 2 = 8
    var graph = ScriptGraph.builder("test-triple-nested", "Triple Nested")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("outer", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("middle", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner_body", "action.print", Map.of("message", "inner"))
      .addNode("inner_cmp", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("inner_result", "flow.result", null)
      .addNode("middle_cmp", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("middle_result", "flow.result", null)
      .addNode("outer_cmp", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("outer_result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      // Execution wiring
      .addExecutionEdge("trigger", "out", "outer", "in")
      .addExecutionEdge("outer", "exec_loop", "middle", "in")
      .addExecutionEdge("middle", "exec_loop", "inner", "in")
      .addExecutionEdge("inner", "exec_loop", "inner_body", "in")
      .addExecutionEdge("inner", "exec_check", "inner_result", "in")
      .addExecutionEdge("middle", "exec_check", "middle_result", "in")
      .addExecutionEdge("outer", "exec_check", "outer_result", "in")
      .addExecutionEdge("outer", "exec_done", "done_print", "in")
      // Data wiring
      .addDataEdge("inner", "index", "inner_cmp", "a")
      .addDataEdge("inner_cmp", "result", "inner_result", "value")
      .addDataEdge("middle", "index", "middle_cmp", "a")
      .addDataEdge("middle_cmp", "result", "middle_result", "value")
      .addDataEdge("outer", "index", "outer_cmp", "a")
      .addDataEdge("outer_cmp", "result", "outer_result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("done_print"),
      "Done print should fire");

    assertEquals(8, countNodeExecutions(listener, "inner_body"),
      "Innermost body should execute 8 times (2*2*2)");
  }

  @Test
  void nestedRepeatUntilInnerMaxIterationsHit() {
    // Outer: index >= 1 → 2 body iterations
    // Inner: maxIterations=3 but condition never met → 3 body iterations
    // Total inner body: 2 * 3 = 6
    var graph = ScriptGraph.builder("test-nested-max", "Nested Max Iterations")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("outer", "flow.repeat_until", Map.of("maxIterations", 100))
      .addNode("inner", "flow.repeat_until", Map.of("maxIterations", 3))
      .addNode("inner_body", "action.print", Map.of("message", "inner"))
      .addNode("false_const", "constant.boolean", Map.of("value", false))
      .addNode("inner_result", "flow.result", null)
      .addNode("outer_compare", "logic.compare", Map.of("b", 1, "operator", ">="))
      .addNode("outer_result", "flow.result", null)
      .addNode("done_print", "action.print", Map.of("message", "done"))
      .addExecutionEdge("trigger", "out", "outer", "in")
      .addExecutionEdge("outer", "exec_loop", "inner", "in")
      .addExecutionEdge("inner", "exec_loop", "inner_body", "in")
      .addExecutionEdge("inner", "exec_check", "inner_result", "in")
      .addExecutionEdge("outer", "exec_check", "outer_result", "in")
      .addExecutionEdge("outer", "exec_done", "done_print", "in")
      .addDataEdge("false_const", "value", "inner_result", "value")
      .addDataEdge("outer", "index", "outer_compare", "a")
      .addDataEdge("outer_compare", "result", "outer_result", "value")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);

    var outerOutputs = listener.nodeOutputs.get("outer");
    assertNotNull(outerOutputs, "Outer should have outputs");
    assertTrue(outerOutputs.get("conditionMet").asBoolean(false),
      "Outer condition should be met");

    assertEquals(6, countNodeExecutions(listener, "inner_body"),
      "Inner body should execute 6 times (2 outer * 3 inner max)");
  }
}
