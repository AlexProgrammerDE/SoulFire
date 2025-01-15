/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {
  private final ServerCommandManager serverCommandManager;

  @Override
  public void executeCommand(
    CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
    SFHelpers.mustSupply(() -> switch (request.getScopeCase()) {
      case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_COMMAND_EXECUTION));
      case INSTANCE -> () -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_EXECUTION, instanceId));

        ServerCommandManager.putInstanceIds(List.of(instanceId));
      };
      case SCOPE_NOT_SET -> () -> {
        throw new IllegalArgumentException("Scope not set");
      };
    });

    try {
      var code = serverCommandManager.execute(request.getCommand(), ServerRPCConstants.USER_CONTEXT_KEY.get());

      responseObserver.onNext(CommandResponse.newBuilder().setCode(code).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error executing command", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void tabCompleteCommand(
    CommandCompletionRequest request,
    StreamObserver<CommandCompletionResponse> responseObserver) {
    SFHelpers.mustSupply(() -> switch (request.getScopeCase()) {
      case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_COMMAND_COMPLETION));
      case INSTANCE -> () -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_COMPLETION, instanceId));

        ServerCommandManager.putInstanceIds(List.of(instanceId));
      };
      case SCOPE_NOT_SET -> () -> {
        throw new IllegalArgumentException("Scope not set");
      };
    });

    try {
      var suggestions = serverCommandManager.complete(request.getCommand(), request.getCursor(), ServerRPCConstants.USER_CONTEXT_KEY.get())
        .stream()
        .map(completion -> {
          var builder = CommandCompletion.newBuilder()
            .setSuggestion(completion.suggestion());
          if (completion.tooltip() != null) {
            builder.setTooltip(completion.tooltip());
          }
          return builder.build();
        })
        .toList();

      responseObserver.onNext(
        CommandCompletionResponse.newBuilder()
          .addAllSuggestions(suggestions)
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error tab completing", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
