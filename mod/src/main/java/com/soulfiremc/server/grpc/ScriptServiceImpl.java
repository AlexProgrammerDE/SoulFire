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
import com.soulfiremc.grpc.generated.PortType;
import com.soulfiremc.grpc.generated.ScriptNode;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.script.PortDefinition;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.HashMap;
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
  private final ScriptTriggerService triggerService = new ScriptTriggerService();

  // Track active scripts: scriptId -> activation state
  private final Map<UUID, ScriptActivationState> activeScripts = new ConcurrentHashMap<>();

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

  private record ScriptActivationState(
    UUID scriptId,
    UUID instanceId,
    boolean active,
    String activeNodeId,
    long activationCount,
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
        newScript.nodesJson(nodesToJson(request.getNodesList()));
        newScript.edgesJson(edgesToJson(request.getEdgesList()));
        newScript.autoStart(request.getAutoStart());

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
        if (request.getUpdateNodes()) {
          script.nodesJson(nodesToJson(request.getNodesList()));
        }
        if (request.getUpdateEdges()) {
          script.edgesJson(edgesToJson(request.getEdgesList()));
        }
        if (request.hasAutoStart()) {
          script.autoStart(request.getAutoStart());
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
      // Deactivate the script if active - unregister triggers first
      triggerService.unregisterTriggers(scriptId);
      var activationState = activeScripts.remove(scriptId);
      if (activationState != null && activationState.observer() != null) {
        try {
          activationState.observer().onNext(ScriptEvent.newBuilder()
            .setScriptCompleted(ScriptCompleted.newBuilder()
              .setScriptId(scriptId.toString())
              .setSuccess(false)
              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
              .build())
            .build());
          activationState.observer().onCompleted();
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
  public void activateScript(ActivateScriptRequest request, StreamObserver<ScriptEvent> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.CHANGE_INSTANCE_STATE, instanceId));

    try {
      // Check if script is already active
      if (activeScripts.containsKey(scriptId)) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Script is already active"));
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
      var activationState = new ScriptActivationState(
        scriptId,
        instanceId,
        true,
        null,
        1,
        serverObserver
      );

      activeScripts.put(scriptId, activationState);

      // Set up cancellation handler
      serverObserver.setOnCancelHandler(() -> {
        activeScripts.remove(scriptId);
        triggerService.unregisterTriggers(scriptId);
        log.info("Script {} deactivated by client disconnect", scriptId);
      });

      // Send script started event
      responseObserver.onNext(ScriptEvent.newBuilder()
        .setScriptStarted(ScriptStarted.newBuilder()
          .setScriptId(scriptId.toString())
          .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
          .build())
        .build());

      // Build the script graph from the entity
      var graph = buildScriptGraph(scriptEntity);

      // Get the instance manager
      var instanceManager = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      // Create event listener that streams events to the client
      var eventListener = createStreamingEventListener(scriptId, serverObserver);

      // Create reactive context for trigger execution
      var context = new ReactiveScriptContext(instanceManager, eventListener);

      // Register triggers for event-driven execution using reactive engine
      // Scripts are now purely reactive - they only respond to trigger events
      var engine = new ReactiveScriptEngine();
      triggerService.registerTriggers(scriptId, graph, context, engine);

      log.info("Script {} activated with {} triggers", scriptId, graph.findTriggerNodes().size());

    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error activating script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deactivateScript(DeactivateScriptRequest request, StreamObserver<DeactivateScriptResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var scriptId = UUID.fromString(request.getScriptId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.CHANGE_INSTANCE_STATE, instanceId));

    try {
      // Unregister any active triggers
      triggerService.unregisterTriggers(scriptId);

      var activationState = activeScripts.remove(scriptId);
      if (activationState == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Script is not active"));
      }

      // Send completion event and close the stream
      if (activationState.observer() != null && !activationState.observer().isCancelled()) {
        try {
          activationState.observer().onNext(ScriptEvent.newBuilder()
            .setScriptCompleted(ScriptCompleted.newBuilder()
              .setScriptId(scriptId.toString())
              .setSuccess(true)
              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
              .build())
            .build());
          activationState.observer().onCompleted();
        } catch (Exception e) {
          log.debug("Error completing script stream on deactivation", e);
        }
      }

      log.info("Script {} deactivated", scriptId);
      responseObserver.onNext(DeactivateScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error deactivating script", t);
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

      var activationState = activeScripts.get(scriptId);
      var statusBuilder = ScriptStatus.newBuilder()
        .setScriptId(scriptId.toString())
        .setIsActive(activationState != null)
        .setActivationCount(activationState != null ? activationState.activationCount() : 0);

      if (activationState != null && activationState.activeNodeId() != null) {
        statusBuilder.setActiveNodeId(activationState.activeNodeId());
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

  @Override
  public void getNodeTypes(GetNodeTypesRequest request, StreamObserver<GetNodeTypesResponse> responseObserver) {
    // No authentication required - node types are public metadata
    try {
      var categoryId = request.hasCategory() ? request.getCategory() : null;
      var includeDeprecated = request.getIncludeDeprecated();

      var metadata = NodeRegistry.getFilteredMetadata(categoryId, includeDeprecated);
      var categories = NodeRegistry.getAllCategories();

      var responseBuilder = GetNodeTypesResponse.newBuilder();

      for (var nodeMeta : metadata) {
        responseBuilder.addNodeTypes(metadataToProto(nodeMeta));
      }

      for (var category : categories) {
        responseBuilder.addCategories(categoryToProto(category));
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting node types", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private CategoryDefinition categoryToProto(NodeCategory category) {
    return CategoryDefinition.newBuilder()
      .setId(category.id())
      .setDisplayName(category.displayName())
      .setIcon(category.icon())
      .setDescription(category.description())
      .setSortOrder(category.sortOrder())
      .build();
  }

  private NodeTypeDefinition metadataToProto(NodeMetadata metadata) {
    var builder = NodeTypeDefinition.newBuilder()
      .setType(metadata.type())
      .setDisplayName(metadata.displayName())
      .setDescription(metadata.description())
      .setCategory(metadata.category().id())
      .setIsTrigger(metadata.isTrigger())
      .addAllKeywords(metadata.keywords())
      .setDeprecated(metadata.deprecated())
      .setIcon(metadata.icon());

    if (metadata.color() != null) {
      builder.setColor(metadata.color());
    }
    if (metadata.deprecationMessage() != null) {
      builder.setDeprecationMessage(metadata.deprecationMessage());
    }

    for (var input : metadata.inputs()) {
      builder.addInputs(portDefinitionToProto(input));
    }
    for (var output : metadata.outputs()) {
      builder.addOutputs(portDefinitionToProto(output));
    }

    return builder.build();
  }

  private com.soulfiremc.grpc.generated.PortDefinition portDefinitionToProto(PortDefinition port) {
    var builder = com.soulfiremc.grpc.generated.PortDefinition.newBuilder()
      .setId(port.id())
      .setDisplayName(port.displayName())
      .setPortType(portTypeToProto(port.type()))
      .setRequired(port.required())
      .setDescription(port.description());

    if (port.defaultValue() != null) {
      builder.setDefaultValue(port.defaultValue());
    }
    if (port.elementType() != null) {
      builder.setElementType(portTypeToProto(port.elementType()));
    }

    return builder.build();
  }

  private PortType portTypeToProto(com.soulfiremc.server.script.PortType type) {
    return switch (type) {
      case ANY -> PortType.PORT_TYPE_ANY;
      case NUMBER -> PortType.PORT_TYPE_NUMBER;
      case STRING -> PortType.PORT_TYPE_STRING;
      case BOOLEAN -> PortType.PORT_TYPE_BOOLEAN;
      case VECTOR3 -> PortType.PORT_TYPE_VECTOR3;
      case BOT -> PortType.PORT_TYPE_BOT;
      case LIST -> PortType.PORT_TYPE_LIST;
      case EXEC -> PortType.PORT_TYPE_EXEC;
      case BLOCK -> PortType.PORT_TYPE_BLOCK;
      case ENTITY -> PortType.PORT_TYPE_ENTITY;
      case ITEM -> PortType.PORT_TYPE_ITEM;
    };
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
      .addAllNodes(jsonToNodes(entity.nodesJson()))
      .addAllEdges(jsonToEdges(entity.edgesJson()))
      .setAutoStart(entity.autoStart())
      .build();
  }

  private ScriptInfo entityToScriptInfo(ScriptEntity entity) {
    return ScriptInfo.newBuilder()
      .setId(entity.id().toString())
      .setName(entity.name())
      .setDescription(entity.description() != null ? entity.description() : "")
      .setInstanceId(entity.instance().id().toString())
      .setCreatedAt(instantToTimestamp(entity.createdAt()))
      .setUpdatedAt(instantToTimestamp(entity.updatedAt()))
      .setAutoStart(entity.autoStart())
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

  private ScriptGraph buildScriptGraph(ScriptEntity entity) {
    var builder = ScriptGraph.builder(entity.id().toString(), entity.name());

    List<ScriptNodeData> nodes = GsonInstance.GSON.fromJson(entity.nodesJson(), NODE_LIST_TYPE);
    List<ScriptEdgeData> edges = GsonInstance.GSON.fromJson(entity.edgesJson(), EDGE_LIST_TYPE);

    // Add nodes
    if (nodes != null) {
      for (var node : nodes) {
        Map<String, Object> defaultInputs = new HashMap<>();
        if (node.data() != null) {
          for (var entry : node.data().entrySet()) {
            defaultInputs.put(entry.getKey(), jsonElementToObject(entry.getValue()));
          }
        }
        builder.addNode(node.id(), node.type(), defaultInputs);
      }
    }

    // Add edges
    if (edges != null) {
      for (var edge : edges) {
        if ("EDGE_TYPE_EXECUTION".equals(edge.edgeType())) {
          builder.addExecutionEdge(edge.source(), edge.sourceHandle(), edge.target(), edge.targetHandle());
        } else {
          builder.addDataEdge(edge.source(), edge.sourceHandle(), edge.target(), edge.targetHandle());
        }
      }
    }

    return builder.build();
  }

  private Object jsonElementToObject(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (element.isJsonPrimitive()) {
      var primitive = element.getAsJsonPrimitive();
      if (primitive.isBoolean()) {
        return primitive.getAsBoolean();
      }
      if (primitive.isNumber()) {
        return primitive.getAsDouble();
      }
      return primitive.getAsString();
    }
    if (element.isJsonArray()) {
      return GsonInstance.GSON.fromJson(element, List.class);
    }
    if (element.isJsonObject()) {
      return GsonInstance.GSON.fromJson(element, Map.class);
    }
    return element.toString();
  }

  private ScriptEventListener createStreamingEventListener(UUID scriptId, ServerCallStreamObserver<ScriptEvent> observer) {
    return new ScriptEventListener() {
      @Override
      public void onNodeStarted(String nodeId) {
        if (!observer.isCancelled()) {
          try {
            observer.onNext(ScriptEvent.newBuilder()
              .setNodeStarted(NodeStarted.newBuilder()
                .setNodeId(nodeId)
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build())
              .build());
          } catch (Exception e) {
            log.debug("Error sending node started event", e);
          }
        }
      }

      @Override
      public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs) {
        if (!observer.isCancelled()) {
          try {
            var builder = NodeCompleted.newBuilder()
              .setNodeId(nodeId)
              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()));
            for (var entry : outputs.entrySet()) {
              builder.putOutputs(entry.getKey(), nodeValueToProtoValue(entry.getValue()));
            }
            observer.onNext(ScriptEvent.newBuilder().setNodeCompleted(builder.build()).build());
          } catch (Exception e) {
            log.debug("Error sending node completed event", e);
          }
        }
      }

      @Override
      public void onNodeError(String nodeId, String error) {
        if (!observer.isCancelled()) {
          try {
            observer.onNext(ScriptEvent.newBuilder()
              .setNodeError(NodeError.newBuilder()
                .setNodeId(nodeId)
                .setErrorMessage(error)
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build())
              .build());
          } catch (Exception e) {
            log.debug("Error sending node error event", e);
          }
        }
      }

      @Override
      public void onScriptCompleted(boolean success) {
        if (!observer.isCancelled()) {
          try {
            observer.onNext(ScriptEvent.newBuilder()
              .setScriptCompleted(ScriptCompleted.newBuilder()
                .setScriptId(scriptId.toString())
                .setSuccess(success)
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build())
              .build());
            observer.onCompleted();
          } catch (Exception e) {
            log.debug("Error sending script completed event", e);
          }
        }
      }

      @Override
      public void onScriptCancelled() {
        activeScripts.remove(scriptId);
        if (!observer.isCancelled()) {
          try {
            observer.onNext(ScriptEvent.newBuilder()
              .setScriptCompleted(ScriptCompleted.newBuilder()
                .setScriptId(scriptId.toString())
                .setSuccess(false)
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build())
              .build());
            observer.onCompleted();
          } catch (Exception e) {
            log.debug("Error sending script cancelled event", e);
          }
        }
      }

      @Override
      public void onLog(String level, String message) {
        log.info("[Script {}] [{}] {}", scriptId, level.toUpperCase(), message);
      }
    };
  }

  private Value objectToProtoValue(Object value) {
    if (value == null) {
      return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
    }
    if (value instanceof Boolean b) {
      return Value.newBuilder().setBoolValue(b).build();
    }
    if (value instanceof Number n) {
      return Value.newBuilder().setNumberValue(n.doubleValue()).build();
    }
    if (value instanceof String s) {
      return Value.newBuilder().setStringValue(s).build();
    }
    if (value instanceof List<?> list) {
      var listBuilder = com.google.protobuf.ListValue.newBuilder();
      for (var item : list) {
        listBuilder.addValues(objectToProtoValue(item));
      }
      return Value.newBuilder().setListValue(listBuilder.build()).build();
    }
    if (value instanceof Map<?, ?> map) {
      var structBuilder = com.google.protobuf.Struct.newBuilder();
      for (var entry : map.entrySet()) {
        structBuilder.putFields(entry.getKey().toString(), objectToProtoValue(entry.getValue()));
      }
      return Value.newBuilder().setStructValue(structBuilder.build()).build();
    }
    return Value.newBuilder().setStringValue(value.toString()).build();
  }

  private Value nodeValueToProtoValue(NodeValue value) {
    if (value == null || value.isNull()) {
      return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
    }
    if (value instanceof NodeValue.Json(JsonElement element)) {
      return jsonElementToProtoValue(element);
    }
    if (value instanceof NodeValue.Bot(com.soulfiremc.server.bot.BotConnection bot1)) {
      // Bot references are serialized as their account name
      return Value.newBuilder().setStringValue(bot1.accountName()).build();
    }
    return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
  }

  private Value jsonElementToProtoValue(com.google.gson.JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
    }
    if (element.isJsonPrimitive()) {
      var primitive = element.getAsJsonPrimitive();
      if (primitive.isBoolean()) {
        return Value.newBuilder().setBoolValue(primitive.getAsBoolean()).build();
      }
      if (primitive.isNumber()) {
        return Value.newBuilder().setNumberValue(primitive.getAsDouble()).build();
      }
      return Value.newBuilder().setStringValue(primitive.getAsString()).build();
    }
    if (element.isJsonArray()) {
      var listBuilder = com.google.protobuf.ListValue.newBuilder();
      for (var item : element.getAsJsonArray()) {
        listBuilder.addValues(jsonElementToProtoValue(item));
      }
      return Value.newBuilder().setListValue(listBuilder.build()).build();
    }
    if (element.isJsonObject()) {
      var structBuilder = com.google.protobuf.Struct.newBuilder();
      for (var entry : element.getAsJsonObject().entrySet()) {
        structBuilder.putFields(entry.getKey(), jsonElementToProtoValue(entry.getValue()));
      }
      return Value.newBuilder().setStructValue(structBuilder.build()).build();
    }
    return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
  }
}
