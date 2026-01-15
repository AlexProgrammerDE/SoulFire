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

import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.script.ScriptLanguage;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class ScriptServiceImpl extends ScriptServiceGrpc.ScriptServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void createScript(CreateScriptRequest request, StreamObserver<CreateScriptResponse> responseObserver) {
    verifyScope(request.getScope());

    try {
      var result = soulFireServer.sessionFactory().fromTransaction(session -> {
        var scriptEntity = new ScriptEntity();
        scriptEntity.type(switch (request.getScope().getScopeCase()) {
          case GLOBAL_SCRIPT -> ScriptEntity.ScriptType.GLOBAL;
          case INSTANCE_SCRIPT -> ScriptEntity.ScriptType.INSTANCE;
          case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
        });
        SFHelpers.mustSupply(() -> switch (request.getScope().getScopeCase()) {
          case GLOBAL_SCRIPT -> () -> {};
          case INSTANCE_SCRIPT -> () -> {
            var instanceId = UUID.fromString(request.getScope().getInstanceScript().getId());
            var instance = session.find(InstanceEntity.class, instanceId);
            if (instance == null) {
              throw new IllegalArgumentException("Instance not found");
            }

            scriptEntity.instance(instance);
          };
          case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
        });
        scriptEntity.scriptName(request.getScriptName());
        if (request.getElevatedPermissions()) {
          ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.ELEVATE_SCRIPT_PERMISSIONS));
          scriptEntity.elevatedPermissions(true);
        }

        session.persist(scriptEntity);

        return scriptEntity;
      });
      var codePath = soulFireServer.getScriptCodePath(result.id());
      Files.createDirectories(codePath);
      Files.writeString(codePath.resolve("main.ts"), """
        // This is a SoulFire script
        console.log('Hello, World!');
        """);

      SFHelpers.mustSupply(() -> switch (request.getScope().getScopeCase()) {
        case GLOBAL_SCRIPT -> () -> soulFireServer.instances().values().forEach(instance -> instance.scriptManager().registerScript(result));
        case INSTANCE_SCRIPT -> () -> {
          var instanceId = UUID.fromString(request.getScope().getInstanceScript().getId());
          var instance = soulFireServer.instances().get(instanceId);
          if (instance == null) {
            throw new IllegalArgumentException("Instance not found");
          }

          instance.scriptManager().registerScript(result);
        };
        case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
      });

      responseObserver.onNext(CreateScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error creating script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deleteScript(DeleteScriptRequest request, StreamObserver<DeleteScriptResponse> responseObserver) {
    try {
      var scriptId = UUID.fromString(request.getId());
      soulFireServer.sessionFactory().inTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null) {
          throw new IllegalArgumentException("Script not found");
        }

        SFHelpers.mustSupply(() -> switch (script.type()) {
          case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.DELETE_GLOBAL_SCRIPT));
          case INSTANCE -> () -> {
            var instanceId = script.instance().id();
            ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DELETE_SCRIPT, instanceId));
          };
        });

        session.remove(script);
      });

      soulFireServer.instances().values().forEach(instance -> instance.scriptManager().unregisterScript(scriptId));

      var codePath = soulFireServer.getScriptCodePath(scriptId);
      try {
        SFHelpers.deleteDirectory(codePath);
      } catch (IOException e) {
        log.error("Error while deleting script code", e);
      }

      responseObserver.onNext(DeleteScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void restartScript(RestartScriptRequest request, StreamObserver<RestartScriptResponse> responseObserver) {
    try {
      var scriptId = UUID.fromString(request.getId());
      var result = soulFireServer.sessionFactory().fromTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null) {
          throw new IllegalArgumentException("Script not found");
        }

        SFHelpers.mustSupply(() -> switch (script.type()) {
          case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_GLOBAL_SCRIPT));
          case INSTANCE -> () -> {
            var instanceId = script.instance().id();
            ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_SCRIPT, instanceId));
          };
        });

        return script;
      });

      soulFireServer.instances().values().forEach(instance -> instance.scriptManager().maybeReRegisterScript(result));

      responseObserver.onNext(RestartScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error restarting script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateScript(UpdateScriptRequest request, StreamObserver<UpdateScriptResponse> responseObserver) {
    try {
      var scriptId = UUID.fromString(request.getId());
      var result = soulFireServer.sessionFactory().fromTransaction(session -> {
        var script = session.find(ScriptEntity.class, scriptId);
        if (script == null) {
          throw new IllegalArgumentException("Script not found");
        }

        SFHelpers.mustSupply(() -> switch (script.type()) {
          case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_GLOBAL_SCRIPT));
          case INSTANCE -> () -> {
            var instanceId = script.instance().id();
            ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_SCRIPT, instanceId));
          };
        });

        script.scriptName(request.getScriptName());
        if (request.getElevatedPermissions() != script.elevatedPermissions()) {
          ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.ELEVATE_SCRIPT_PERMISSIONS));
        }

        script.elevatedPermissions(request.getElevatedPermissions());

        session.merge(script);

        return script;
      });

      soulFireServer.instances().values().forEach(instance -> instance.scriptManager().maybeReRegisterScript(result));

      responseObserver.onNext(UpdateScriptResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating script", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void listScripts(ScriptListRequest request, StreamObserver<ScriptListResponse> responseObserver) {
    verifyScope(request.getScope());

    try {
      var scripts = soulFireServer.sessionFactory()
        .fromTransaction(session -> session.createQuery("from ScriptEntity", ScriptEntity.class).list());

      var response = ScriptListResponse.newBuilder();
      for (var script : scripts) {
        if (switch (request.getScope().getScopeCase()) {
          case GLOBAL_SCRIPT -> true;
          case INSTANCE_SCRIPT -> script.type() == ScriptEntity.ScriptType.INSTANCE
            && script.instance().id().equals(UUID.fromString(request.getScope().getInstanceScript().getId()));
          case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
        }) {
          response.addScripts(ScriptListResponse.Script.newBuilder()
            .setId(script.id().toString())
            .setScriptName(script.scriptName())
            .setElevatedPermissions(script.elevatedPermissions())
            .setLanguage(switch (ScriptLanguage.determineLanguage(soulFireServer.getScriptCodePath(script.id()))) {
              case JAVASCRIPT -> com.soulfiremc.grpc.generated.ScriptLanguage.JAVASCRIPT;
              case PYTHON -> com.soulfiremc.grpc.generated.ScriptLanguage.PYTHON;
              case TYPESCRIPT -> com.soulfiremc.grpc.generated.ScriptLanguage.TYPESCRIPT;
            })
            .setCreatedAt(Timestamps.fromMillis(script.createdAt().toEpochMilli()))
            .setUpdatedAt(Timestamps.fromMillis(script.updatedAt().toEpochMilli()))
            .setScriptScope(switch (script.type()) {
              case INSTANCE -> ScriptScope.newBuilder()
                .setInstanceScript(InstanceScriptScope.newBuilder()
                  .setId(script.instance().id().toString())
                  .build())
                .build();
              case GLOBAL -> ScriptScope.newBuilder()
                .setGlobalScript(GlobalScriptScope.newBuilder().build())
                .build();
            })
            .build());
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing scripts", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private void verifyScope(ScriptScope scope) {
    switch (scope.getScopeCase()) {
      case GLOBAL_SCRIPT -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_GLOBAL_SCRIPT));
      case INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstanceScript().getId());
        ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_SCRIPT, instanceId));
      }
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    }
  }
}
