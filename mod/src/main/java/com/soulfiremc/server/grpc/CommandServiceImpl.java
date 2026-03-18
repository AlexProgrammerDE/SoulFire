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

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.event.Level;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {
  private final SoulFireServer soulFire;

  @Override
  public void executeCommand(
    CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
    var stack = validateScope(request.getScope());

    try {
      publishCommandEvent(request.getScope(), stack.source(), request.getCommand());
      var code = soulFire.serverCommandManager().execute(request.getCommand(), stack);

      responseObserver.onNext(CommandResponse.newBuilder().setCode(code).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error executing command", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void tabCompleteCommand(
    CommandCompletionRequest request,
    StreamObserver<CommandCompletionResponse> responseObserver) {
    var stack = validateScope(request.getScope());

    try {
      var suggestions = soulFire.serverCommandManager().complete(request.getCommand(), request.getCursor(), stack)
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
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  private CommandSourceStack validateScope(CommandScope scope) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    return switch (scope.getScopeCase()) {
      case GLOBAL -> {
        user.hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_COMMAND_EXECUTION));
        yield CommandSourceStack.ofUnrestricted(soulFire, new EventAwareUser(soulFire, user, null, null));
      }
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_EXECUTION, instanceId));

        yield CommandSourceStack.ofInstance(
          soulFire,
          new EventAwareUser(soulFire, user, instanceId, null),
          Set.of(instanceId));
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getBot().getInstanceId());
        user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_COMMAND_EXECUTION, instanceId));

        var botId = UUID.fromString(scope.getBot().getBotId());

        yield CommandSourceStack.ofInstanceAndBot(
          soulFire,
          new EventAwareUser(soulFire, user, instanceId, botId),
          Set.of(instanceId),
          Set.of(botId));
      }
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };
  }

  private void publishCommandEvent(CommandScope scope, SoulFireUser actor, String command) {
    switch (scope.getScopeCase()) {
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        var instance = soulFire.getInstance(instanceId).orElse(null);
        soulFire.eventStateHolder().publishCommandEvent(
          instanceId,
          instance != null ? instance.friendlyNameCache().get() : null,
          null,
          null,
          actor,
          command);
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getBot().getInstanceId());
        var botId = UUID.fromString(scope.getBot().getBotId());
        var instance = soulFire.getInstance(instanceId).orElse(null);
        var botName = instance != null
          ? instance.getConnectedBots().stream()
          .filter(bot -> bot.accountProfileId().equals(botId))
          .map(BotConnection::accountName)
          .findFirst()
          .orElse(null)
          : null;
        soulFire.eventStateHolder().publishCommandEvent(
          instanceId,
          instance != null ? instance.friendlyNameCache().get() : null,
          botId,
          botName,
          actor,
          command);
      }
      case GLOBAL ->
        soulFire.eventStateHolder().publishCommandEvent(null, null, null, null, actor, command);
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    }
  }

  private record EventAwareUser(
    SoulFireServer soulFire,
    SoulFireUser delegate,
    @Nullable UUID fallbackInstanceId,
    @Nullable UUID fallbackBotId
  ) implements SoulFireUser {
    @Override
    public void sendMessage(Level level, Component message) {
      var instanceName = fallbackInstanceId == null
        ? null
        : soulFire.getInstance(fallbackInstanceId)
        .map(instance -> instance.friendlyNameCache().get())
        .orElse(null);
      var botName = fallbackInstanceId == null || fallbackBotId == null
        ? null
        : soulFire.getInstance(fallbackInstanceId)
        .stream()
        .flatMap(instance -> instance.getConnectedBots().stream())
        .filter(bot -> bot.accountProfileId().equals(fallbackBotId))
        .map(BotConnection::accountName)
        .findFirst()
        .orElse(null);

      try (var ignored = soulFire.eventStateHolder()
        .pushMessageContext(fallbackInstanceId, instanceName, fallbackBotId, botName)) {
        delegate.sendMessage(level, message);
      }
    }

    @Override
    public TriState getPermission(PermissionContext permission) {
      return delegate.getPermission(permission);
    }

    @Override
    public UUID getUniqueId() {
      return delegate.getUniqueId();
    }

    @Override
    public String getUsername() {
      return delegate.getUsername();
    }

    @Override
    public String getEmail() {
      return delegate.getEmail();
    }

    @Override
    public com.soulfiremc.server.database.UserRole getRole() {
      return delegate.getRole();
    }

    @Override
    public Instant getIssuedAt() {
      return delegate.getIssuedAt();
    }
  }
}
