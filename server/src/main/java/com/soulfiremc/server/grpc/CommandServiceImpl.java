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
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.command.ServerCommandManager;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {
  private final SoulFireServer soulFire;
  private final ServerCommandManager serverCommandManager;

  @Override
  public void executeCommand(
    CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    var stack = switch (request.getScopeCase()) {
      case GLOBAL -> {
        user.hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_COMMAND_EXECUTION));
        yield CommandSourceStack.ofUnrestricted(soulFire, user);
      }
      case INSTANCE -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_EXECUTION, instanceId));

        yield CommandSourceStack.ofInstance(soulFire, user, Set.of(instanceId));
      }
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };

    try {
      var code = serverCommandManager.execute(request.getCommand(), stack);

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
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    var stack = switch (request.getScopeCase()) {
      case GLOBAL -> {
        user.hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_COMMAND_COMPLETION));
        yield CommandSourceStack.ofUnrestricted(soulFire, user);
      }
      case INSTANCE -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_COMPLETION, instanceId));

        yield CommandSourceStack.ofInstance(soulFire, user, Set.of(instanceId));
      }
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };

    try {
      var suggestions = serverCommandManager.complete(request.getCommand(), request.getCursor(), stack)
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
