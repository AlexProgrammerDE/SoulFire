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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC service implementation for visual script management.
 * Handles CRUD operations for scripts and script execution lifecycle.
 */
@Slf4j
@RequiredArgsConstructor
public final class ScriptServiceImpl extends ScriptServiceGrpc.ScriptServiceImplBase {
  private static final Type NODE_LIST_TYPE = new TypeToken<List<ScriptNodeData>>() {}.getType();
  private static final Type EDGE_LIST_TYPE = new TypeToken<List<ScriptEdgeData>>() {}.getType();

  private final SoulFireServer soulFireServer;

  // Track running scripts: scriptId -> execution state
  private final Map<UUID, ScriptExecutionState> runningScripts = new ConcurrentHashMap<>();

  // Internal data classes for JSON serialization
  private record ScriptNodeData(
    String id,
    String type,
    PositionData position,
    Map<String, JsonElement> data
  ) {}

  private record PositionData(double x, double y) {}

  private record ScriptEdgeData(
    String id,
    String source,
    String sourceHandle,
    String target,
    String targetHandle,
    String edgeType
  ) {}

  private record ScriptExecutionState(
    UUID scriptId,
    UUID instanceId,
    boolean running,
    String activeNodeId,
    long executionCount,
    ServerCallStreamObserver<ScriptEvent> observer
  ) {}

  @Override
  public void createScript(CreateScriptRequest request, StreamObserver<CreateScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var scriptEntity = soulFireServer.sessionFactory().fromTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        var newScript = new ScriptEntity();
        newScript.name(request.getName());
        newScript.description(request.getDescription());
        newScript.instance(instanceEntity);
        newScript.scope(request.getScope());
        newScript.nodesJson(nodesToJson(request.getNodesList()));
        newScript.edgesJson(edgesToJson(request.getEdgesList()));

        session.persist(newScript);
        return newScript;
      });

      responseObserver.onNext(CreateScriptResponse.newBuilder()
        .setScript(entityToScriptData(scriptEntity))
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error creating script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getScript(GetScriptRequest request, StreamObserver<GetScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      var scriptEntity = soulFireServer.sessionFactory().fromTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }
        return script;
      });

      responseObserver.onNext(GetScriptResponse.newBuilder()
        .setScript(entityToScriptData(scriptEntity))
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateScript(UpdateScriptRequest request, StreamObserver<UpdateScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var scriptEntity = soulFireServer.sessionFactory().fromTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }

        if (request.hasName()) {
          script.name(request.getName());
        }
        if (request.hasDescription()) {
          script.description(request.getDescription());
        }
        if (request.hasScope()) {
          script.scope(request.getScope());
        }
        if (request.getUpdateNodes()) {
          script.nodesJson(nodesToJson(request.getNodesList()));
        }
        if (request.getUpdateEdges()) {
          script.edgesJson(edgesToJson(request.getEdgesList()));
        }

        session.merge(script);
        return script;
      });

      responseObserver.onNext(UpdateScriptResponse.newBuilder()
        .setScript(entityToScriptData(scriptEntity))
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deleteScript(DeleteScriptRequest request, StreamObserver<DeleteScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      // Stop the script if it's running
      var executionState = runningScripts.remove(scriptId);
      if (executionState != null && executionState.observer() != null) {
        try {
          executionState.observer().onNext(ScriptEvent.newBuilder()
            .setScriptCompleted(ScriptCompleted.newBuilder()
              .setSuccess(false)
              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
              .build())
            .build());
          executionState.observer().onCompleted();
        } catch (Exception ignored) {
          // Observer may already be closed
        }
      }

      soulFireServer.sessionFactory().inTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }
        session.remove(script);
      });

      responseObserver.onNext(DeleteScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error deleting script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void listScripts(ListScriptsRequest request, StreamObserver<ListScriptsResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      var scripts = soulFireServer.sessionFactory().fromTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        return session.createQuery("FROM ScriptEntity WHERE instance = :instance ORDER BY createdAt DESC", ScriptEntity.class)
          .setParameter("instance", instanceEntity)
          .list();
      });

      var responseBuilder = ListScriptsResponse.newBuilder();
      for (var script : scripts) {
        responseBuilder.addScripts(entityToScriptInfo(script));
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error listing scripts", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void startScript(StartScriptRequest request, StreamObserver<ScriptEvent> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.CHANGE_INSTANCE_STATE, instanceId));

    try {
      // Check if script is already running
      if (runningScripts.containsKey(scriptId)) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Script is already running"));
      }

      var scriptEntity = soulFireServer.sessionFactory().fromTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }
        return script;
      });

      var serverObserver = (ServerCallStreamObserver<ScriptEvent>) responseObserver;
      var executionState = new ScriptExecutionState(
        scriptId,
        instanceId,
        true,
        null,
        1,
        serverObserver
      );

      runningScripts.put(scriptId, executionState);

      // Set up cancellation handler
      serverObserver.setOnCancelHandler(() -> {
        runningScripts.remove(scriptId);
        log.info("Script {} execution cancelled by client", scriptId);
      });

      // Send script started event
      responseObserver.onNext(ScriptEvent.newBuilder()
        .setScriptStarted(ScriptStarted.newBuilder()
          .setScriptId(scriptId.toString())
          .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
          .build())
        .build());

      // TODO: Implement actual script execution engine
      // For now, we just complete immediately with success
      // The visual script engine would interpret the nodes/edges and execute them
      soulFireServer.scheduler().schedule(() -> {
        var state = runningScripts.remove(scriptId);
        if (state != null && state.observer() != null && !state.observer().isCancelled()) {
          try {
            state.observer().onNext(ScriptEvent.newBuilder()
              .setScriptCompleted(ScriptCompleted.newBuilder()
                .setSuccess(true)
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build())
              .build());
            state.observer().onCompleted();
          } catch (Exception e) {
            log.debug("Error completing script stream", e);
          }
        }
      }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);

    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error starting script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void stopScript(StopScriptRequest request, StreamObserver<StopScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.CHANGE_INSTANCE_STATE, instanceId));

    try {
      var executionState = runningScripts.remove(scriptId);
      if (executionState == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Script is not running"));
      }

      // Send completion event and close the stream
      if (executionState.observer() != null && !executionState.observer().isCancelled()) {
        try {
          executionState.observer().onNext(ScriptEvent.newBuilder()
            .setScriptCompleted(ScriptCompleted.newBuilder()
              .setSuccess(false)
              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
              .build())
            .build());
          executionState.observer().onCompleted();
        } catch (Exception e) {
          log.debug("Error completing script stream on stop", e);
        }
      }

      responseObserver.onNext(StopScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error stopping script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getScriptStatus(GetScriptStatusRequest request, StreamObserver<GetScriptStatusResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      // Verify script exists
      soulFireServer.sessionFactory().inTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }
      });

      var executionState = runningScripts.get(scriptId);
      var statusBuilder = ScriptStatus.newBuilder()
        .setScriptId(scriptId.toString())
        .setIsRunning(executionState != null)
        .setExecutionCount(executionState != null ? executionState.executionCount() : 0);

      if (executionState != null && executionState.activeNodeId() != null) {
        statusBuilder.setActiveNodeId(executionState.activeNodeId());
      }

      responseObserver.onNext(GetScriptStatusResponse.newBuilder()
        .setStatus(statusBuilder.build())
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting script status", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void subscribeScriptLogs(SubscribeScriptLogsRequest request, StreamObserver<ScriptLogEntry> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));

    try {
      // Verify script exists
      soulFireServer.sessionFactory().inTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null || !script.instance().id().equals(instanceId)) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription(
            "Script '%s' not found in instance '%s'".formatted(scriptId, instanceId)));
        }
      });

      var serverObserver = (ServerCallStreamObserver<ScriptLogEntry>) responseObserver;

      // TODO: Implement log subscription via the logging system
      // This would hook into SFLogAppender to filter logs by script ID
      // For now, we just keep the stream open until cancelled

      serverObserver.setOnCancelHandler(() ->
        log.debug("Script log subscription cancelled for script {}", scriptId));

    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error subscribing to script logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private String nodesToJson(List<ScriptNode> nodes) {
    var nodeDataList = nodes.stream()
      .map(node -> new ScriptNodeData(
        node.getId(),
        node.getType(),
        new PositionData(node.getPosition().getX(), node.getPosition().getY()),
        node.getDataMap().entrySet().stream()
          .collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            e -> valueToJsonElement(e.getValue())
          ))
      ))
      .toList();
    return GsonInstance.GSON.toJson(nodeDataList);
  }

  private String edgesToJson(List<ScriptEdge> edges) {
    var edgeDataList = edges.stream()
      .map(edge -> new ScriptEdgeData(
        edge.getId(),
        edge.getSource(),
        edge.getSourceHandle(),
        edge.getTarget(),
        edge.getTargetHandle(),
        edge.getEdgeType().name()
      ))
      .toList();
    return GsonInstance.GSON.toJson(edgeDataList);
  }

  private List<ScriptNode> jsonToNodes(String json) {
    List<ScriptNodeData> nodeDataList = GsonInstance.GSON.fromJson(json, NODE_LIST_TYPE);
    if (nodeDataList == null) {
      return List.of();
    }
    return nodeDataList.stream()
      .map(data -> {
        var nodeBuilder = ScriptNode.newBuilder()
          .setId(data.id())
          .setType(data.type())
          .setPosition(Position.newBuilder()
            .setX(data.position().x())
            .setY(data.position().y())
            .build());

        if (data.data() != null) {
          data.data().forEach((key, jsonElement) ->
            nodeBuilder.putData(key, jsonElementToValue(jsonElement)));
        }

        return nodeBuilder.build();
      })
      .toList();
  }

  private List<ScriptEdge> jsonToEdges(String json) {
    List<ScriptEdgeData> edgeDataList = GsonInstance.GSON.fromJson(json, EDGE_LIST_TYPE);
    if (edgeDataList == null) {
      return List.of();
    }
    return edgeDataList.stream()
      .map(data -> ScriptEdge.newBuilder()
        .setId(data.id())
        .setSource(data.source())
        .setSourceHandle(data.sourceHandle())
        .setTarget(data.target())
        .setTargetHandle(data.targetHandle())
        .setEdgeType(EdgeType.valueOf(data.edgeType()))
        .build())
      .toList();
  }

  private ScriptData entityToScriptData(ScriptEntity entity) {
    return ScriptData.newBuilder()
      .setId(entity.id().toString())
      .setName(entity.name())
      .setDescription(entity.description() != null ? entity.description() : "")
      .setInstanceId(entity.instance().id().toString())
      .setScope(entity.scope())
      .addAllNodes(jsonToNodes(entity.nodesJson()))
      .addAllEdges(jsonToEdges(entity.edgesJson()))
      .build();
  }

  private ScriptInfo entityToScriptInfo(ScriptEntity entity) {
    return ScriptInfo.newBuilder()
      .setId(entity.id().toString())
      .setName(entity.name())
      .setDescription(entity.description() != null ? entity.description() : "")
      .setInstanceId(entity.instance().id().toString())
      .setScope(entity.scope())
      .setCreatedAt(instantToTimestamp(entity.createdAt()))
      .setUpdatedAt(instantToTimestamp(entity.updatedAt()))
      .build();
  }

  private Timestamp instantToTimestamp(java.time.Instant instant) {
    return Timestamps.fromMillis(instant.toEpochMilli());
  }

  private JsonElement valueToJsonElement(Value value) {
    try {
      return GsonInstance.GSON.fromJson(JsonFormat.printer().print(value), JsonElement.class);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  private Value jsonElementToValue(JsonElement element) {
    try {
      var builder = Value.newBuilder();
      JsonFormat.parser().merge(GsonInstance.GSON.toJson(element), builder);
      return builder.build();
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }
}
