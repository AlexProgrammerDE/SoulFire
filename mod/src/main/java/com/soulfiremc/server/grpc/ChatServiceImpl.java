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

import com.mojang.brigadier.suggestion.Suggestions;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.bot.BotThreadExecution;
import com.soulfiremc.server.bot.CompletableControlTask;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.util.StringUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/// Typed chat, command, whisper, and completion operations for an online bot.
public final class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
  private static final Duration ACTION_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(5);

  private final SoulFireServer server;
  private final RpcIdempotencyStore<ChatActionResponse> idempotency =
    new RpcIdempotencyStore<>();

  public ChatServiceImpl(SoulFireServer server) {
    this.server = server;
  }

  @Override
  public void sendPublicChat(
    SendPublicChatRequest request,
    StreamObserver<ChatActionResponse> responseObserver
  ) {
    runAction(
      request.getScope(),
      "public-chat",
      request.hasIdempotencyKey() ? request.getIdempotencyKey() : null,
      request.getMessage(),
      "SDK send public chat",
      () -> {
        var message = requireText(
          request.getMessage(),
          "message",
          SharedConstants.MAX_CHAT_LENGTH
        );
        return bot -> bot.sendPublicChatMessage(message);
      },
      responseObserver
    );
  }

  @Override
  public void sendCommand(
    SendCommandRequest request,
    StreamObserver<ChatActionResponse> responseObserver
  ) {
    runAction(
      request.getScope(),
      "command",
      request.hasIdempotencyKey() ? request.getIdempotencyKey() : null,
      request.getCommand(),
      "SDK send command",
      () -> {
        var command = requireText(
          request.getCommand(),
          "command",
          SharedConstants.MAX_USER_INPUT_COMMAND_LENGTH
        );
        if (command.startsWith("/")) {
          command = command.substring(1);
        }
        if (command.isBlank()) {
          throw invalid("command must contain text after the leading slash");
        }
        var normalized = command;
        return bot -> bot.sendCommand(normalized);
      },
      responseObserver
    );
  }

  @Override
  public void sendWhisper(
    SendWhisperRequest request,
    StreamObserver<ChatActionResponse> responseObserver
  ) {
    runAction(
      request.getScope(),
      "whisper",
      request.hasIdempotencyKey() ? request.getIdempotencyKey() : null,
      request.getRecipient() + "\u0000" + request.getMessage(),
      "SDK send whisper",
      () -> {
        var recipient = requireText(
          request.getRecipient(),
          "recipient",
          SharedConstants.MAX_PLAYER_NAME_LENGTH
        );
        if (!recipient.matches("[A-Za-z0-9_]+")) {
          throw invalid("recipient must be a valid Minecraft player name");
        }
        var message = requireText(
          request.getMessage(),
          "message",
          SharedConstants.MAX_CHAT_LENGTH
        );
        var command = "msg %s %s".formatted(recipient, message);
        return bot -> bot.sendCommand(command);
      },
      responseObserver
    );
  }

  @Override
  public void tabComplete(
    TabCompleteRequest request,
    StreamObserver<TabCompleteResponse> responseObserver
  ) {
    try {
      var bot = requireBot(request.getScope(), InstancePermission.READ_BOT_INFO, false);
      var input = request.getInput();
      var cursor = request.hasCursor() ? request.getCursor() : input.length();
      if (cursor < 0 || cursor > input.length()) {
        throw invalid("cursor must be between zero and the input length");
      }
      var future = inBot(bot, () -> suggestions(bot, input, cursor));
      future
        .orTimeout(COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        .whenComplete((suggestions, error) -> {
          if (error != null) {
            responseObserver.onError(toGrpcError(error));
            return;
          }
          responseObserver.onNext(toResponse(suggestions, input.startsWith("/")));
          responseObserver.onCompleted();
        });
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  private void runAction(
    ChatScope scope,
    String operation,
    @Nullable String idempotencyKey,
    String fingerprint,
    String description,
    Supplier<BotAction> actionSupplier,
    StreamObserver<ChatActionResponse> responseObserver
  ) {
    try {
      var bot = requireBot(
        scope,
        InstancePermission.CONTROL_BOT_ACTIONS,
        true
      );
      var action = actionSupplier.get();
      CompletableFuture<ChatActionResponse> future;
      CompletableControlTask task = null;
      if (idempotencyKey == null) {
        task = submit(bot, description, action);
        future = response(task);
      } else {
        future = idempotency.execute(
          ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(),
          scope.getInstanceId(),
          scope.getBotId(),
          operation,
          idempotencyKey,
          fingerprint,
          () -> {
            try {
              return response(submit(bot, description, action));
            } catch (Exception exception) {
              return CompletableFuture.failedFuture(exception);
            }
          }
        );
      }

      var serverObserver =
        (ServerCallStreamObserver<ChatActionResponse>) responseObserver;
      if (task != null) {
        var ownedTask = task;
        serverObserver.setOnCancelHandler(() -> bot.botControl().cancel(ownedTask));
      }
      future.whenComplete((result, error) -> {
        if (serverObserver.isCancelled()) {
          return;
        }
        if (error != null) {
          responseObserver.onError(toGrpcError(error));
          return;
        }
        responseObserver.onNext(result);
        responseObserver.onCompleted();
      });
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  private BotConnection requireBot(
    ChatScope scope,
    InstancePermission permission,
    boolean requireControl
  ) {
    if (scope.getInstanceId().isBlank() || scope.getBotId().isBlank()) {
      throw invalid("scope.instance_id and scope.bot_id are required");
    }
    var instanceId = parseUuid(scope.getInstanceId(), "scope.instance_id");
    var botId = parseUuid(scope.getBotId(), "scope.bot_id");
    ServerRPCConstants.USER_CONTEXT_KEY.get()
      .hasPermissionOrThrow(PermissionContext.instance(permission, instanceId));
    var instance = server.getInstance(instanceId)
      .orElseThrow(() -> Status.NOT_FOUND
        .withDescription("Instance '%s' not found".formatted(instanceId))
        .asRuntimeException());
    if (requireControl) {
      try {
        instance.botControlLeaseManager().authorize(
          botId,
          ServerRPCConstants.BOT_CONTROL_TOKEN_CONTEXT_KEY.get()
        );
      } catch (BotControlLeaseManager.InvalidLeaseException exception) {
        throw Status.PERMISSION_DENIED
          .withDescription(exception.getMessage())
          .asRuntimeException();
      }
    }
    var bot = instance.botConnections().get(botId);
    if (bot == null || bot.isDisconnected()) {
      throw Status.FAILED_PRECONDITION
        .withDescription("Bot '%s' is not online".formatted(botId))
        .asRuntimeException();
    }
    return bot;
  }

  private static CompletableControlTask submit(
    BotConnection bot,
    String description,
    BotAction action
  ) throws Exception {
    var task = new CompletableControlTask(ControlTask.once(
      description,
      Set.of(ControlResource.CHAT),
      () -> action.run(bot)
    ));
    inBot(bot, () -> {
      bot.botControl().replace(task);
      return null;
    });
    return task;
  }

  private static CompletableFuture<ChatActionResponse> response(
    CompletableControlTask task
  ) {
    return task.completion()
      .orTimeout(ACTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
      .thenApply(reason -> ChatActionResponse.newBuilder()
        .setResult(BotActionResult.newBuilder()
          .setActionId(task.actionId().toString())
          .setStatus(switch (reason) {
            case COMPLETED, CLAIMED ->
              BotActionStatus.BOT_ACTION_STATUS_COMPLETED;
            case CANCELLED, REPLACED ->
              BotActionStatus.BOT_ACTION_STATUS_CANCELLED;
            case FAILED -> BotActionStatus.BOT_ACTION_STATUS_FAILED;
          }))
        .build())
      .exceptionally(error -> ChatActionResponse.newBuilder()
        .setResult(BotActionResult.newBuilder()
          .setActionId(task.actionId().toString())
          .setStatus(BotActionStatus.BOT_ACTION_STATUS_FAILED)
          .setError(errorMessage(error)))
        .build());
  }

  private static CompletableFuture<Suggestions> suggestions(
    BotConnection bot,
    String input,
    int cursor
  ) {
    var connection = Objects.requireNonNull(
      bot.minecraft().getConnection(),
      "Bot is not connected"
    );
    if (input.startsWith("/")) {
      var command = input.substring(1);
      var commandCursor = Math.max(0, cursor - 1);
      var dispatcher = connection.getCommands();
      return dispatcher.getCompletionSuggestions(
        dispatcher.parse(command, connection.getSuggestionsProvider()),
        commandCursor
      );
    }

    var start = input.lastIndexOf(' ', Math.max(0, cursor - 1)) + 1;
    var prefix = input.substring(start, cursor).toLowerCase(Locale.ROOT);
    var suggestions = connection.getSuggestionsProvider()
      .getCustomTabSuggestions()
      .stream()
      .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
      .map(value -> new com.mojang.brigadier.suggestion.Suggestion(
        new com.mojang.brigadier.context.StringRange(start, cursor),
        value
      ))
      .toList();
    return CompletableFuture.completedFuture(
      Suggestions.create(input, suggestions)
    );
  }

  private static TabCompleteResponse toResponse(
    Suggestions suggestions,
    boolean leadingSlash
  ) {
    var offset = leadingSlash ? 1 : 0;
    var response = TabCompleteResponse.newBuilder()
      .setStart(suggestions.getRange().getStart() + offset)
      .setLength(suggestions.getRange().getLength());
    for (var suggestion : suggestions.getList()) {
      var item = TabSuggestion.newBuilder().setText(suggestion.getText());
      if (suggestion.getTooltip() != null) {
        item.setTooltipJson(GsonComponentSerializer.gson().serialize(
          Component.text(suggestion.getTooltip().getString())
        ));
      }
      response.addSuggestions(item);
    }
    return response.build();
  }

  static String requireText(String value, String field, int maxLength) {
    if (value.isBlank()) {
      throw invalid(field + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw invalid(field + " must be at most " + maxLength + " characters");
    }
    if (!value.chars().allMatch(StringUtil::isAllowedChatCharacter)) {
      throw invalid(field + " contains characters that Minecraft does not allow");
    }
    return value;
  }

  private static UUID parseUuid(String value, String field) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw invalid(field + " must be a UUID");
    }
  }

  private static <T> T inBot(
    BotConnection bot,
    java.util.concurrent.Callable<T> action
  ) throws Exception {
    return BotThreadExecution.call(bot, action);
  }

  private static RuntimeException invalid(String description) {
    return Status.INVALID_ARGUMENT
      .withDescription(description)
      .asRuntimeException();
  }

  private static RuntimeException toGrpcError(Throwable throwable) {
    var cause = unwrap(throwable);
    if (cause instanceof StatusRuntimeException statusError) {
      return statusError;
    }
    if (cause instanceof TimeoutException) {
      return Status.DEADLINE_EXCEEDED
        .withDescription("Operation timed out")
        .withCause(cause)
        .asRuntimeException();
    }
    return Status.INTERNAL
      .withDescription(errorMessage(cause))
      .withCause(cause)
      .asRuntimeException();
  }

  private static Throwable unwrap(Throwable throwable) {
    var current = throwable;
    while ((current instanceof CompletionException
      || current instanceof ExecutionException)
      && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }

  private static String errorMessage(Throwable throwable) {
    var cause = unwrap(throwable);
    return Objects.requireNonNullElse(
      cause.getMessage(),
      cause.getClass().getSimpleName()
    );
  }

  @FunctionalInterface
  private interface BotAction {
    void run(BotConnection bot);
  }

}
