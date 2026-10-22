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
package com.soulfiremc.server.grpc;

import com.google.gson.JsonArray;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.soulfiremc.grpc.generated.ActivateScriptRequest;
import com.soulfiremc.grpc.generated.CreateScriptRequest;
import com.soulfiremc.grpc.generated.CreateScriptResponse;
import com.soulfiremc.grpc.generated.DeactivateScriptRequest;
import com.soulfiremc.grpc.generated.DryRunScriptRequest;
import com.soulfiremc.grpc.generated.EdgeType;
import com.soulfiremc.grpc.generated.GetScriptRequest;
import com.soulfiremc.grpc.generated.GetScriptResponse;
import com.soulfiremc.grpc.generated.GetScriptStatusRequest;
import com.soulfiremc.grpc.generated.GetScriptStatusResponse;
import com.soulfiremc.grpc.generated.PortType;
import com.soulfiremc.grpc.generated.Position;
import com.soulfiremc.grpc.generated.ScriptEdge;
import com.soulfiremc.grpc.generated.ScriptEvent;
import com.soulfiremc.grpc.generated.ScriptNode;
import com.soulfiremc.grpc.generated.ScriptQuotas;
import com.soulfiremc.grpc.generated.UpdateScriptRequest;
import com.soulfiremc.grpc.generated.UpdateScriptResponse;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.CategoryRegistry;
import com.soulfiremc.server.script.NodeMetadata;
import com.soulfiremc.server.script.NodeRuntime;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.PortDefinition;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.soulfiremc.shared.SFLogAppender;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.minecraft.world.phys.Vec3;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sqlite.SQLiteDataSource;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ScriptExecutionServiceTest {
  private final SoulFireServer server = mock();
  private final ScriptServiceImpl service = new ScriptServiceImpl(server);
  private final Context rpcContext = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));
  private final String instanceId = UUID.randomUUID().toString();
  private final String triggerId = UUID.randomUUID().toString();
  private final String actionId = UUID.randomUUID().toString();
  private final ServerCallStreamObserver<ScriptEvent> observer = mock();
  private final List<ScriptEvent> events = new CopyOnWriteArrayList<>();
  private final AtomicReference<Runnable> cancel = new AtomicReference<>();
  private final AtomicReference<Throwable> error = new AtomicReference<>();
  private final CountDownLatch terminal = new CountDownLatch(1);

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("scripts.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    when(server.dsl()).thenReturn(DSL.using(source, SQLDialect.SQLITE));
    var ownerId = UUID.randomUUID().toString();
    server.dsl().execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId, "owner", "owner@example.com", "ADMIN");
    server.dsl().insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId).set(Tables.INSTANCES.FRIENDLY_NAME, "scripts")
      .set(Tables.INSTANCES.OWNER_ID, ownerId).set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.serializeToTree())).execute();
    doAnswer(call -> { cancel.set(call.getArgument(0)); return null; }).when(observer).setOnCancelHandler(any());
    doAnswer(call -> { events.add(call.getArgument(0)); return null; }).when(observer).onNext(any());
    doAnswer(_ -> { terminal.countDown(); return null; }).when(observer).onCompleted();
    doAnswer(call -> { error.set(call.getArgument(0)); terminal.countDown(); return null; }).when(observer).onError(any());
  }

  private String saveScript(String actionType) {
    return saveScript(actionType, "trigger.on_chat");
  }

  private String saveScript(String actionType, String triggerType) {
    var response = new AtomicReference<CreateScriptResponse>();
    StreamObserver<CreateScriptResponse> created = mock();
    doAnswer(call -> { response.set(call.getArgument(0)); return null; }).when(created).onNext(any());
    var request = CreateScriptRequest.newBuilder().setInstanceId(instanceId).setName("dry run").setPaused(true)
      .addNodes(ScriptNode.newBuilder().setId(triggerId).setType(triggerType))
      .addNodes(ScriptNode.newBuilder().setId(actionId).setType(actionType))
      .addEdges(ScriptEdge.newBuilder().setId(UUID.randomUUID().toString())
        .setSource(triggerId).setSourceHandle("out").setTarget(actionId).setTargetHandle("in"))
      .build();
    rpcContext.run(() -> service.createScript(request, created));
    return response.get().getScript().getId();
  }

  private DryRunScriptRequest.Builder request(String scriptId) {
    return DryRunScriptRequest.newBuilder().setInstanceId(instanceId).setScriptId(scriptId).setTriggerNodeId(triggerId);
  }

  private void run(DryRunScriptRequest request) {
    rpcContext.run(() -> service.dryRunScript(request, observer));
  }

  private void awaitCompletion() throws InterruptedException {
    assertTrue(terminal.await(5, TimeUnit.SECONDS));
    assertNull(error.get());
  }

  @Test
  void preservesExplicitNullMembersAcrossCreateReadAndUpdate() {
    var nil = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    var nested = Value.newBuilder().setStructValue(Struct.newBuilder()
      .putFields("missing", nil)
      .putFields("items", Value.newBuilder().setListValue(ListValue.newBuilder()
        .addValues(nil)
        .addValues(Value.newBuilder().setStructValue(Struct.newBuilder().putFields("missing", nil)))
        .addValues(Value.newBuilder().setBoolValue(false))
        .addValues(Value.newBuilder().setNumberValue(0))
        .addValues(Value.newBuilder().setStringValue(""))).build())).build();
    var node = ScriptNode.newBuilder().setId(actionId).setType("json.stringify")
      .putData("value", nested).putData("nullable", nil).build();
    var created = this.<CreateScriptResponse>rpcResponse(observer -> service.createScript(CreateScriptRequest.newBuilder()
      .setInstanceId(instanceId).setName("null values").setPaused(true).addNodes(node).build(), observer));
    var scriptId = created.getScript().getId();
    assertEquals(node.getDataMap(), created.getScript().getNodes(0).getDataMap());
    var read = this.<GetScriptResponse>rpcResponse(observer -> service.getScript(GetScriptRequest.newBuilder()
      .setInstanceId(instanceId).setScriptId(scriptId).build(), observer));
    assertEquals(node.getDataMap(), read.getScript().getNodes(0).getDataMap());

    var changed = node.toBuilder().putData("added", nested).build();
    var updated = this.<UpdateScriptResponse>rpcResponse(observer -> service.updateScript(UpdateScriptRequest.newBuilder()
      .setInstanceId(instanceId).setScriptId(scriptId).setUpdateNodes(true).addNodes(changed).build(), observer));
    assertEquals(changed.getDataMap(), updated.getScript().getNodes(0).getDataMap());
    var reread = this.<GetScriptResponse>rpcResponse(observer -> service.getScript(GetScriptRequest.newBuilder()
      .setInstanceId(instanceId).setScriptId(scriptId).build(), observer));
    assertEquals(changed.getDataMap(), reread.getScript().getNodes(0).getDataMap());
  }

  private <T> T rpcResponse(Consumer<StreamObserver<T>> action) {
    var response = new AtomicReference<T>();
    var failure = new AtomicReference<Throwable>();
    StreamObserver<T> observer = mock();
    doAnswer(call -> { response.set(call.getArgument(0)); return null; }).when(observer).onNext(any());
    doAnswer(call -> { failure.set(call.getArgument(0)); return null; }).when(observer).onError(any());
    rpcContext.run(() -> action.accept(observer));
    assertNull(failure.get());
    return response.get();
  }

  @Test
  void preservesNodeMetadataAndEdgeOrderAcrossSaveAndReload() {
    var frameId = UUID.randomUUID().toString();
    var frame = ScriptNode.newBuilder().setId(frameId).setType("layout.frame")
      .setPosition(Position.newBuilder().setX(-20).setY(-30))
      .setWidth(512.5).setHeight(321).setLabel("frame").addContainedNodes(actionId).build();
    var node = ScriptNode.newBuilder().setId(actionId).setType("action.print")
      .setPosition(Position.newBuilder().setX(10).setY(20))
      .setMuted(true).setCollapsed(true).setLabel("muted print").setWidth(0).setHeight(234.5)
      .setParentFrameId(frameId).setResolvedType(PortType.PORT_TYPE_STRING).build();
    var trigger = ScriptNode.newBuilder().setId(triggerId).setType("trigger.on_chat")
      .setPosition(Position.getDefaultInstance()).build();
    var edge = ScriptEdge.newBuilder().setId(UUID.randomUUID().toString())
      .setSource(triggerId).setSourceHandle("out").setTarget(actionId).setTargetHandle("in")
      .setEdgeType(EdgeType.EDGE_TYPE_EXECUTION).setOrder(9).build();
    var nodes = List.of(trigger, node, frame);
    var created = this.<CreateScriptResponse>rpcResponse(observer -> service.createScript(CreateScriptRequest.newBuilder()
      .setInstanceId(instanceId).setName("metadata").setPaused(true).addAllNodes(nodes).addEdges(edge).build(), observer));
    assertEquals(nodes, created.getScript().getNodesList());
    assertEquals(List.of(edge), created.getScript().getEdgesList());
    var scriptId = created.getScript().getId();
    for (var explicitZero : List.of(false, true)) {
      var changedBuilder = node.toBuilder().setMuted(false).setCollapsed(false).setLabel("")
        .clearWidth().clearHeight().clearContainedNodes().setParentFrameId("")
        .clearResolvedType();
      var changedEdgeBuilder = edge.toBuilder().clearOrder();
      if (explicitZero) {
        changedBuilder.setResolvedTypeValue(0);
        changedEdgeBuilder.setOrder(0);
      }
      var changed = changedBuilder.build();
      var changedNodes = List.of(trigger, changed, frame);
      var changedEdge = changedEdgeBuilder.build();
      var updated = this.<UpdateScriptResponse>rpcResponse(observer -> service.updateScript(UpdateScriptRequest.newBuilder()
        .setInstanceId(instanceId).setScriptId(scriptId).setUpdateNodes(true).addAllNodes(changedNodes)
        .setUpdateEdges(true).addEdges(changedEdge).build(), observer));
      assertEquals(changedNodes, updated.getScript().getNodesList());
      assertEquals(List.of(changedEdge), updated.getScript().getEdgesList());
      var read = this.<GetScriptResponse>rpcResponse(observer -> service.getScript(GetScriptRequest.newBuilder()
        .setInstanceId(instanceId).setScriptId(scriptId).build(), observer));
      assertEquals(changedNodes, read.getScript().getNodesList());
      assertEquals(List.of(changedEdge), read.getScript().getEdgesList());
    }
  }

  @Test
  void readsStoredGraphsWithoutOptionalMetadata() {
    var scriptId = saveScript("action.print");
    var record = server.dsl().selectFrom(Tables.SCRIPTS).where(Tables.SCRIPTS.ID.eq(scriptId)).fetchSingle();
    var nodes = GsonInstance.GSON.fromJson(record.getNodesJson(), JsonArray.class);
    for (var node : nodes) {
      node.getAsJsonObject().keySet().retainAll(List.of("id", "type", "position", "data"));
    }
    server.dsl().update(Tables.SCRIPTS).set(Tables.SCRIPTS.NODES_JSON, GsonInstance.CONFIG_GSON.toJson(nodes))
      .where(Tables.SCRIPTS.ID.eq(scriptId)).execute();
    var read = this.<GetScriptResponse>rpcResponse(observer -> service.getScript(GetScriptRequest.newBuilder()
      .setInstanceId(instanceId).setScriptId(scriptId).build(), observer));
    assertEquals(2, read.getScript().getNodesCount());
    for (var node : read.getScript().getNodesList()) {
      assertFalse(node.getMuted());
      assertFalse(node.getCollapsed());
      assertFalse(node.hasWidth());
      assertFalse(node.hasHeight());
      assertFalse(node.hasResolvedType());
      assertTrue(node.getContainedNodesList().isEmpty());
      assertTrue(node.getLabel().isEmpty());
      assertTrue(node.getParentFrameId().isEmpty());
    }
    assertFalse(read.getScript().getEdges(0).hasOrder());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void savedMutedNodesBypassExecutionAndKeepDownstreamNodesRunning(boolean insideGroup) throws InterruptedException {
    var groupId = UUID.randomUUID().toString();
    var afterId = UUID.randomUUID().toString();
    var muted = ScriptNode.newBuilder().setId(actionId).setType("action.print").setMuted(true)
      .putData("message", Value.newBuilder().setStringValue("skip").build());
    if (insideGroup) {
      muted.putData("parentGroupId", Value.newBuilder().setStringValue(groupId).build());
    }
    var request = CreateScriptRequest.newBuilder().setInstanceId(instanceId).setName("muted node").setPaused(true)
      .addNodes(ScriptNode.newBuilder().setId(triggerId).setType("trigger.on_chat"))
      .addNodes(muted)
      .addNodes(ScriptNode.newBuilder().setId(afterId).setType("action.print")
        .putData("message", Value.newBuilder().setStringValue("after").build()))
      .addEdges(ScriptEdge.newBuilder().setId(UUID.randomUUID().toString())
        .setSource(triggerId).setSourceHandle("out").setTarget(actionId).setTargetHandle("in"))
      .addEdges(ScriptEdge.newBuilder().setId(UUID.randomUUID().toString())
        .setSource(actionId).setSourceHandle("out").setTarget(afterId).setTargetHandle("in"));
    if (insideGroup) {
      request.addNodes(ScriptNode.newBuilder().setId(groupId).setType("layout.group"));
    }
    var created = this.<CreateScriptResponse>rpcResponse(observer -> service.createScript(request.build(), observer));
    run(request(created.getScript().getId()).build());
    awaitCompletion();
    assertTrue(events.getLast().getScriptCompleted().getSuccess());
    assertEquals(List.of("after"), events.stream().filter(ScriptEvent::hasScriptLog)
      .map(event -> event.getScriptLog().getMessage()).toList());
    assertTrue(events.stream().filter(ScriptEvent::hasNodeCompleted)
      .anyMatch(event -> event.getNodeCompleted().getNodeId().equals(actionId)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"debug", "info", "warn", "error"})
  void automaticallyStartedScriptsStreamEventsAndRetainScopedLogs(String level) throws InterruptedException {
    var scheduler = new SoulFireScheduler(runnable -> runnable);
    var instance = mock(InstanceManager.class);
    when(instance.scheduler()).thenReturn(scheduler);
    when(server.getInstance(UUID.fromString(instanceId))).thenReturn(Optional.of(instance));
    var scriptId = saveScript("action.print", "trigger.on_interval");
    var logEvents = new CopyOnWriteArrayList<SFLogAppender.SFLogEvent>();
    Consumer<SFLogAppender.SFLogEvent> consumer = entry -> {
      if (UUID.fromString(scriptId).equals(entry.scriptId())) {
        logEvents.add(entry);
      }
    };
    SFLogAppender.INSTANCE.start();
    SFLogAppender.INSTANCE.logConsumers().add(consumer);
    var logReceived = new CountDownLatch(1);
    doAnswer(call -> {
      ScriptEvent event = call.getArgument(0);
      events.add(event);
      if (event.hasScriptLog()) {
        logReceived.countDown();
      }
      return null;
    }).when(observer).onNext(any());
    try {
      var update = UpdateScriptRequest.newBuilder().setInstanceId(instanceId).setScriptId(scriptId)
        .setPaused(false).setUpdateNodes(true)
        .addNodes(ScriptNode.newBuilder().setId(triggerId).setType("trigger.on_interval")
          .putData("intervalMs", Value.newBuilder().setNumberValue(50).build()))
        .addNodes(ScriptNode.newBuilder().setId(actionId).setType("action.print")
          .putData("message", Value.newBuilder().setStringValue("monitoring").build())
          .putData("level", Value.newBuilder().setStringValue(level).build()))
        .build();
      rpcContext.run(() -> service.updateScript(update, mock()));
      var activation = ActivateScriptRequest.newBuilder().setInstanceId(instanceId).setScriptId(scriptId).build();
      rpcContext.run(() -> service.activateScript(activation, observer));

      assertTrue(logReceived.await(5, TimeUnit.SECONDS));
      assertTrue(events.stream().anyMatch(ScriptEvent::hasScriptStarted));
      assertFalse(logEvents.isEmpty());
      assertEquals(UUID.fromString(instanceId), logEvents.getFirst().instanceId());
      assertEquals(level.toUpperCase(java.util.Locale.ROOT), logEvents.getFirst().level());
    } finally {
      rpcContext.run(() -> service.deactivateScript(DeactivateScriptRequest.newBuilder()
        .setInstanceId(instanceId).setScriptId(scriptId).build(), mock()));
      SFLogAppender.INSTANCE.logConsumers().remove(consumer);
      scheduler.shutdown();
    }
    verify(observer, times(1)).onCompleted();
    assertEquals(1, events.stream().filter(ScriptEvent::hasScriptCompleted).count());
  }

  @Test
  void stoppingAnInstanceCompletesItsObserversAndKeepsOtherInstancesActive() {
    var otherInstance = UUID.randomUUID().toString();
    var owner = server.dsl().selectFrom(Tables.INSTANCES)
      .where(Tables.INSTANCES.ID.eq(instanceId)).fetchSingle().getOwnerId();
    server.dsl().insertInto(Tables.INSTANCES).set(Tables.INSTANCES.ID, otherInstance)
      .set(Tables.INSTANCES.FRIENDLY_NAME, "other").set(Tables.INSTANCES.OWNER_ID, owner)
      .set(Tables.INSTANCES.ICON, "pickaxe").set(Tables.INSTANCES.SETTINGS, "{}").execute();
    when(server.getInstance(any(UUID.class))).thenReturn(Optional.of(mock(InstanceManager.class)));
    var first = this.<CreateScriptResponse>rpcResponse(o -> service.createScript(CreateScriptRequest.newBuilder()
      .setInstanceId(instanceId).setName("first").setPaused(true).build(), o)).getScript().getId();
    var second = this.<CreateScriptResponse>rpcResponse(o -> service.createScript(CreateScriptRequest.newBuilder()
      .setInstanceId(otherInstance).setName("second").setPaused(true).build(), o)).getScript().getId();
    ServerCallStreamObserver<ScriptEvent> otherObserver = mock();
    try {
      rpcContext.run(() -> service.activateScript(ActivateScriptRequest.newBuilder()
        .setInstanceId(instanceId).setScriptId(first).build(), observer));
      rpcContext.run(() -> service.activateScript(ActivateScriptRequest.newBuilder()
        .setInstanceId(otherInstance).setScriptId(second).build(), otherObserver));
      service.stopAllScripts(UUID.fromString(instanceId));
      service.stopAllScripts(UUID.fromString(instanceId));
      verify(observer, times(1)).onCompleted();
      assertEquals(1, events.stream().filter(ScriptEvent::hasScriptCompleted).count());
      assertFalse(events.getLast().getScriptCompleted().getSuccess());
      verify(otherObserver, never()).onCompleted();
      assertFalse(this.<GetScriptStatusResponse>rpcResponse(o -> service.getScriptStatus(GetScriptStatusRequest.newBuilder()
        .setInstanceId(instanceId).setScriptId(first).build(), o)).getStatus().getIsActive());
      assertTrue(this.<GetScriptStatusResponse>rpcResponse(o -> service.getScriptStatus(GetScriptStatusRequest.newBuilder()
        .setInstanceId(otherInstance).setScriptId(second).build(), o)).getStatus().getIsActive());
    } finally {
      service.stopAllScripts(UUID.fromString(otherInstance));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectedActivationPreservesThePausedFlag(boolean missingInstance) {
    var scriptId = saveScript(missingInstance ? "action.print" : "unknown.node");
    when(server.getInstance(UUID.fromString(instanceId))).thenReturn(
      missingInstance ? Optional.empty() : Optional.of(mock(InstanceManager.class)));
    var request = ActivateScriptRequest.newBuilder().setInstanceId(instanceId).setScriptId(scriptId).build();

    var failure = assertThrows(StatusRuntimeException.class,
      () -> rpcContext.run(() -> service.activateScript(request, observer)));

    assertEquals(missingInstance ? Status.Code.NOT_FOUND : Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
    assertTrue(server.dsl().selectFrom(Tables.SCRIPTS).where(Tables.SCRIPTS.ID.eq(scriptId)).fetchSingle().getPaused());
    verify(observer, never()).onNext(any());
  }

  @Test
  void preservesStructuredMocksOutputsAndTerminalOrdering() throws InterruptedException {
    var scriptId = saveScript("action.print");
    var nested = Value.newBuilder().setStructValue(Struct.newBuilder().putFields("values",
      Value.newBuilder().setListValue(ListValue.newBuilder()
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE))
        .addValues(Value.newBuilder().setBoolValue(true))
        .addValues(Value.newBuilder().setNumberValue(2.5))).build())).build();
    run(request(scriptId).putMockInputs("message", nested).build());
    awaitCompletion();
    var completed = events.stream().filter(ScriptEvent::hasNodeCompleted).map(ScriptEvent::getNodeCompleted)
      .filter(event -> event.getNodeId().equals(triggerId)).findFirst().orElseThrow();
    assertEquals(nested, completed.getOutputsOrThrow("message"));
    assertTrue(completed.hasTimestamp());
    assertTrue(events.stream().filter(ScriptEvent::hasNodeStarted).allMatch(event -> event.getNodeStarted().hasTimestamp()));
    var last = events.getLast().getScriptCompleted();
    assertTrue(last.getSuccess());
    assertEquals(scriptId, last.getScriptId());
    assertTrue(last.hasTimestamp());
    assertTrue(events.get(events.size() - 2).hasExecutionStats());
    verify(observer, times(1)).onCompleted();
    verify(observer, never()).onError(any());
  }

  @Test
  void rejectsMissingAndNonTriggerNodes() {
    var scriptId = saveScript("action.print");
    for (var id : List.of(UUID.randomUUID().toString(), actionId)) {
      var failure = assertThrows(StatusRuntimeException.class, () -> run(request(scriptId).setTriggerNodeId(id).build()));
      assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
    }
    assertTrue(events.isEmpty());
  }

  @Test
  void mocksOverrideGeneratedTriggerOutputs() throws InterruptedException {
    var scriptId = saveScript("action.print", "trigger.on_script_init");
    run(request(scriptId).putMockInputs("timestamp", Value.newBuilder().setNumberValue(42).build()).build());
    awaitCompletion();
    var trigger = events.stream().filter(ScriptEvent::hasNodeCompleted).map(ScriptEvent::getNodeCompleted)
      .filter(event -> event.getNodeId().equals(triggerId)).findFirst().orElseThrow();
    assertEquals(42, trigger.getOutputsOrThrow("timestamp").getNumberValue());
  }

  @Test
  void honorsSavedQuotasAndKeepsTheScriptPaused() throws InterruptedException {
    var scriptId = saveScript("action.print");
    rpcContext.run(() -> service.updateScript(UpdateScriptRequest.newBuilder()
      .setInstanceId(instanceId).setScriptId(scriptId).setUpdateQuotas(true)
      .setQuotas(ScriptQuotas.newBuilder().setMaxExecutionCount(0)).build(), mock()));
    run(request(scriptId).build());
    awaitCompletion();
    assertTrue(events.stream().anyMatch(ScriptEvent::hasNodeError));
    assertFalse(events.getLast().getScriptCompleted().getSuccess());
    assertEquals(0, events.stream().filter(ScriptEvent::hasExecutionStats).findFirst().orElseThrow().getExecutionStats().getMaxCount());
    assertTrue(server.dsl().selectFrom(Tables.SCRIPTS).where(Tables.SCRIPTS.ID.eq(scriptId)).fetchSingle().getPaused());
  }

  @Test
  void rejectsInvalidGraphs() {
    var scriptId = saveScript("unknown.node");
    var failure = assertThrows(StatusRuntimeException.class, () -> run(request(scriptId).build()));
    assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
  }

  private String registerAction(Supplier<Mono<Map<String, NodeValue>>> execution) {
    var type = "test.dry_run_" + UUID.randomUUID();
    NodeRegistry.register(NodeMetadata.builder().type(type).displayName("test").category(CategoryRegistry.ACTIONS)
      .addInputs(PortDefinition.execIn()).addOutputs(PortDefinition.execOut()).build(),
      () -> new AbstractScriptNode() {
        @Override
        public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
          return execution.get();
        }
      });
    return type;
  }

  @Test
  void nodeErrorsProduceFailedCompletionWithoutDoubleTermination() throws InterruptedException {
    var type = registerAction(() -> Mono.error(new IllegalStateException()));
    run(request(saveScript(type)).build());
    awaitCompletion();
    assertTrue(events.stream().anyMatch(ScriptEvent::hasNodeError));
    assertFalse(events.getLast().getScriptCompleted().getSuccess());
    verify(observer, times(1)).onCompleted();
    verify(observer, never()).onError(any());
  }

  @Test
  void disconnectDisposesExecutionAndDoesNotCompleteTheCancelledStream() throws InterruptedException {
    var started = new CountDownLatch(1);
    var cancelled = new CountDownLatch(1);
    var type = registerAction(() -> Mono.defer(() -> {
      started.countDown();
      return Mono.<Map<String, NodeValue>>never().doOnCancel(cancelled::countDown);
    }));
    run(request(saveScript(type)).build());
    assertTrue(started.await(5, TimeUnit.SECONDS));
    cancel.get().run();
    assertTrue(cancelled.await(5, TimeUnit.SECONDS));
    verify(observer, never()).onCompleted();
    verify(observer, never()).onError(any());
  }

  @Test
  void cancellationDuringHandlerRegistrationDoesNotStartExecution() {
    var started = new CountDownLatch(1);
    var type = registerAction(() -> { started.countDown(); return Mono.empty(); });
    var scriptId = saveScript(type);
    doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(observer).setOnCancelHandler(any());
    run(request(scriptId).build());
    assertEquals(1, started.getCount());
    assertTrue(events.isEmpty());
    verify(observer, never()).onCompleted();
  }

  @Test
  void serializesVectorAndCollectionOutputs() throws InterruptedException {
    var type = registerAction(() -> Mono.just(Map.of(
      "vector", NodeValue.ofVector3(new Vec3(1, 2, 3)),
      "collection", NodeValue.ofSet(List.of(NodeValue.ofNumber(4), NodeValue.ofNumber(4), NodeValue.ofNumber(5)))
    )));
    run(request(saveScript(type)).build());
    awaitCompletion();
    var outputs = events.stream().filter(ScriptEvent::hasNodeCompleted).map(ScriptEvent::getNodeCompleted)
      .filter(event -> event.getNodeId().equals(actionId)).findFirst().orElseThrow().getOutputsMap();
    assertEquals(3, outputs.get("vector").getStructValue().getFieldsCount());
    assertEquals(2, outputs.get("vector").getStructValue().getFieldsOrThrow("y").getNumberValue());
    assertEquals(List.of(4.0, 5.0), outputs.get("collection").getListValue().getValuesList().stream().map(Value::getNumberValue).toList());
  }
}
