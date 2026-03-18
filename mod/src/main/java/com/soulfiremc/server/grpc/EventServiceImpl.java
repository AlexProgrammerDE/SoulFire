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

import com.google.common.base.Supplier;
import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.*;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.server.util.structs.SafeCloseable;
import com.soulfiremc.shared.SFLogAppender;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.event.Level;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public final class EventServiceImpl extends EventsServiceGrpc.EventsServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void getPrevious(PreviousEventRequest request, StreamObserver<PreviousEventResponse> responseObserver) {
    validateScopeAccess(request.getScope());

    try {
      var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
      var predicate = eventPredicate(request.getScope(), user.getUniqueId());
      responseObserver.onNext(PreviousEventResponse.newBuilder()
        .addAllEvents(soulFireServer.eventStateHolder().events().getNewest(request.getCount())
          .stream()
          .filter(predicate::test)
          .toList())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting previous events", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void subscribe(EventRequest request, StreamObserver<EventResponse> responseObserver) {
    validateScopeAccess(request.getScope());

    try {
      var userId = ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId();
      var issuedAt = ServerRPCConstants.USER_CONTEXT_KEY.get().getIssuedAt();
      var user = new CachedLazyObject<>((Supplier<Optional<SoulFireUser>>)
        () -> soulFireServer.authSystem().authenticateBySubject(userId, issuedAt), 1, TimeUnit.SECONDS);
      var predicate = eventPredicate(request.getScope(), userId);
      new StateHolder.ConnectionMessageSender(
        soulFireServer.eventStateHolder(),
        userId,
        (ServerCallStreamObserver<EventResponse>) responseObserver,
        event -> user.get().filter(soulFireUser -> hasScopeAccess(soulFireUser, request.getScope())
          && predicate.test(event)).isPresent()
      );
    } catch (Throwable t) {
      log.error("Error subscribing to events", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  private void validateScopeAccess(EventScope scope) {
    if (!hasScopeAccess(ServerRPCConstants.USER_CONTEXT_KEY.get(), scope)) {
      throw Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource")
        .asRuntimeException();
    }
  }

  private boolean hasScopeAccess(SoulFireUser user, EventScope scope) {
    return switch (scope.getScopeCase()) {
      case GLOBAL -> user.hasPermission(PermissionContext.global(GlobalPermission.GLOBAL_SUBSCRIBE_LOGS));
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getBot().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstanceScript().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case PERSONAL -> true;
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };
  }

  private EventPredicate eventPredicate(EventScope scope, UUID userId) {
    return switch (scope.getScopeCase()) {
      case GLOBAL -> event -> !event.getPersonal();
      case INSTANCE -> {
        var instanceId = scope.getInstance().getInstanceId();
        yield event -> !event.getPersonal() && instanceId.equals(event.getInstanceId());
      }
      case BOT -> {
        var instanceId = scope.getBot().getInstanceId();
        var botId = scope.getBot().getBotId();
        yield event -> !event.getPersonal()
          && instanceId.equals(event.getInstanceId())
          && botId.equals(event.getBotAccountId());
      }
      case INSTANCE_SCRIPT -> {
        var instanceId = scope.getInstanceScript().getInstanceId();
        var scriptId = scope.getInstanceScript().getScriptId();
        yield event -> !event.getPersonal()
          && instanceId.equals(event.getInstanceId())
          && scriptId.equals(event.getScriptId());
      }
      case PERSONAL -> event -> event.getPersonal() && userId.toString().equals(event.getUserId());
      case SCOPE_NOT_SET -> _ -> false;
    };
  }

  @FunctionalInterface
  public interface EventPredicate {
    boolean test(EventEntry event);
  }

  public static final class StateHolder {
    private static final int MAX_EVENTS = 1_000;
    private static final long AGGREGATION_WINDOW_MS = 15_000;
    private static final Pattern ANSI_ESCAPE_PATTERN =
      Pattern.compile("\\u001B\\[[;\\d?]*[ -/]*[@-~]");
    private static final AtomicInteger EVENT_COUNTER = new AtomicInteger();
    private static final ThreadLocal<MessageContext> MESSAGE_CONTEXT = new ThreadLocal<>();

    private final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> joinedBots = new ConcurrentHashMap<>();
    private final Map<String, AggregateState> recentAggregates = new ConcurrentHashMap<>();
    private final SFLogAppender.QueueWithMaxSize<EventEntry> events = new SFLogAppender.QueueWithMaxSize<>(MAX_EVENTS);
    private final Object publishLock = new Object();

    public StateHolder() {
      SoulFireAPI.registerListener(PreBotConnectEvent.class, this::onPreBotConnect);
      SoulFireAPI.registerListener(BotPostEntityTickEvent.class, this::onBotPostEntityTick);
      SoulFireAPI.registerListener(BotDisconnectedEvent.class, this::onBotDisconnected);
      SoulFireAPI.registerListener(ChatMessageReceiveEvent.class, this::onChatMessage);
      SFLogAppender.INSTANCE.logConsumers().add(this::bridgeLogMessage);
    }

    public SFLogAppender.QueueWithMaxSize<EventEntry> events() {
      return events;
    }

    public SafeCloseable pushMessageContext(
      @Nullable UUID instanceId,
      @Nullable String instanceName,
      @Nullable UUID botId,
      @Nullable String botName
    ) {
      return SFHelpers.smartThreadLocalCloseable(
        MESSAGE_CONTEXT,
        new MessageContext(instanceId, instanceName, botId, botName));
    }

    public void publishSessionEvent(
      InstanceManager instanceManager,
      EventType type,
      EventSeverity severity,
      String summary,
      @Nullable String body,
      @Nullable SoulFireUser actor
    ) {
      publish(let(
        baseBuilder(
          nextId(),
          System.currentTimeMillis(),
          severity,
          EventCategory.EVENT_CATEGORY_SESSION,
          type,
          summary,
          body,
          false)
          .setInstanceId(instanceManager.id().toString())
          .setInstanceName(instanceManager.friendlyNameCache().get()),
        builder -> applyUser(builder, actor)
      ).build());
    }

    public void publishCommandEvent(
      @Nullable UUID instanceId,
      @Nullable String instanceName,
      @Nullable UUID botId,
      @Nullable String botName,
      SoulFireUser actor,
      String command
    ) {
      publish(let(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        EventSeverity.EVENT_SEVERITY_INFO,
        EventCategory.EVENT_CATEGORY_COMMAND,
        EventType.EVENT_TYPE_COMMAND_EXECUTED,
        "Command executed",
        null,
        instanceId == null), builder -> {
          if (instanceId != null) {
            builder.setInstanceId(instanceId.toString());
          }
          if (instanceName != null) {
            builder.setInstanceName(instanceName);
          }
          if (botId != null) {
            builder.setBotAccountId(botId.toString());
          }
          if (botName != null) {
            builder.setBotAccountName(botName);
          }
          if (instanceId == null) {
            builder.setUserId(actor.getUniqueId().toString());
          }
          builder.setCommand(command);
          applyUser(builder, actor);
        }).build());
    }

    public void publishMessage(
      UUID targetUserId,
      @Nullable SoulFireUser actor,
      Level level,
      String message
    ) {
      var instance = InstanceManager.currentOptional().orElse(null);
      var bot = BotConnection.currentOptional().orElse(null);
      var context = MESSAGE_CONTEXT.get();
      var resolvedInstanceId = instance != null ? instance.id() : context != null ? context.instanceId() : null;
      var resolvedInstanceName = instance != null
        ? instance.friendlyNameCache().get()
        : context != null
          ? context.instanceName()
          : null;
      var resolvedBotId = bot != null ? bot.accountProfileId() : context != null ? context.botId() : null;
      var resolvedBotName = bot != null ? bot.accountName() : context != null ? context.botName() : null;
      var personal = resolvedInstanceId == null;
      publish(let(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        toSeverity(level),
        EventCategory.EVENT_CATEGORY_MESSAGE,
        EventType.EVENT_TYPE_MESSAGE,
        message,
        null,
        personal), builder -> {
          if (resolvedInstanceId != null) {
            builder.setInstanceId(resolvedInstanceId.toString());
          }
          if (resolvedInstanceName != null) {
            builder.setInstanceName(resolvedInstanceName);
          }
          if (resolvedBotId != null) {
            builder.setBotAccountId(resolvedBotId.toString());
          }
          if (resolvedBotName != null) {
            builder.setBotAccountName(resolvedBotName);
          }
          builder.setUserId(targetUserId.toString());
          applyUser(builder, actor);
        }).build());
    }

    public void publishScriptEvent(
      InstanceManager instanceManager,
      UUID scriptId,
      String scriptName,
      EventSeverity severity,
      EventType type,
      String summary,
      @Nullable String body
    ) {
      var bot = BotConnection.current();
      publish(let(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        severity,
        EventCategory.EVENT_CATEGORY_SCRIPT,
        type,
        summary,
        body,
        false)
        .setInstanceId(instanceManager.id().toString())
        .setInstanceName(instanceManager.friendlyNameCache().get())
        .setScriptId(scriptId.toString())
        .setScriptName(scriptName), builder -> {
          if (bot != null) {
            builder.setBotAccountId(bot.accountProfileId().toString());
            builder.setBotAccountName(bot.accountName());
          }
        }).build());
    }

    private void onPreBotConnect(PreBotConnectEvent event) {
      publish(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        EventSeverity.EVENT_SEVERITY_INFO,
        EventCategory.EVENT_CATEGORY_BOT,
        EventType.EVENT_TYPE_BOT_CONNECTING,
        "%s is connecting".formatted(event.connection().accountName()),
        event.connection().serverAddress().toString(),
        false)
        .setInstanceId(event.instanceManager().id().toString())
        .setInstanceName(event.instanceManager().friendlyNameCache().get())
        .setBotAccountId(event.connection().accountProfileId().toString())
        .setBotAccountName(event.connection().accountName())
        .build());
    }

    private void onBotPostEntityTick(BotPostEntityTickEvent event) {
      var connection = event.connection();
      if (connection.minecraft().player == null) {
        return;
      }

      if (joinedBots.putIfAbsent(connection.accountProfileId(), true) != null) {
        return;
      }

      publish(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        EventSeverity.EVENT_SEVERITY_SUCCESS,
        EventCategory.EVENT_CATEGORY_BOT,
        EventType.EVENT_TYPE_BOT_JOINED,
        "%s joined the world".formatted(connection.accountName()),
        connection.serverAddress().toString(),
        false)
        .setInstanceId(connection.instanceManager().id().toString())
        .setInstanceName(connection.instanceManager().friendlyNameCache().get())
        .setBotAccountId(connection.accountProfileId().toString())
        .setBotAccountName(connection.accountName())
        .build());
    }

    private void onBotDisconnected(BotDisconnectedEvent event) {
      joinedBots.remove(event.connection().accountProfileId());

      publish(baseBuilder(
        nextId(),
        System.currentTimeMillis(),
        EventSeverity.EVENT_SEVERITY_WARN,
        EventCategory.EVENT_CATEGORY_BOT,
        EventType.EVENT_TYPE_BOT_DISCONNECTED,
        "%s disconnected".formatted(event.connection().accountName()),
        SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(event.message()),
        false)
        .setInstanceId(event.instanceManager().id().toString())
        .setInstanceName(event.instanceManager().friendlyNameCache().get())
        .setBotAccountId(event.connection().accountProfileId().toString())
        .setBotAccountName(event.connection().accountName())
        .build());
    }

    private void onChatMessage(ChatMessageReceiveEvent event) {
      publish(baseBuilder(
        nextId(),
        event.timestamp(),
        EventSeverity.EVENT_SEVERITY_INFO,
        EventCategory.EVENT_CATEGORY_CHAT,
        EventType.EVENT_TYPE_CHAT_MESSAGE,
        event.connection().accountName(),
        event.parseToPlainText(),
        false)
        .setInstanceId(event.instanceManager().id().toString())
        .setInstanceName(event.instanceManager().friendlyNameCache().get())
        .setBotAccountId(event.connection().accountProfileId().toString())
        .setBotAccountName(event.connection().accountName())
        .build());
    }

    private void bridgeLogMessage(SFLogAppender.SFLogEvent event) {
      if (event.level() == null) {
        return;
      }

      var severity = switch (event.level()) {
        case "WARN" -> EventSeverity.EVENT_SEVERITY_WARN;
        case "ERROR", "FATAL" -> EventSeverity.EVENT_SEVERITY_ERROR;
        default -> null;
      };
      if (severity == null) {
        return;
      }

      var summarizedMessage = summarizeSystemMessage(stripAnsi(event.message()));
      if (summarizedMessage.summary().isBlank()) {
        return;
      }

      publish(let(baseBuilder(
        event.id(),
        event.timestamp(),
        severity,
        EventCategory.EVENT_CATEGORY_SYSTEM,
        severity == EventSeverity.EVENT_SEVERITY_WARN
          ? EventType.EVENT_TYPE_SYSTEM_WARNING
          : EventType.EVENT_TYPE_SYSTEM_ERROR,
        summarizedMessage.summary(),
        summarizedMessage.body(),
        false), builder -> {
          if (event.instanceId() != null) {
            builder.setInstanceId(event.instanceId().toString());
          }
          if (event.instanceName() != null) {
            builder.setInstanceName(event.instanceName());
          }
          if (event.botAccountId() != null) {
            builder.setBotAccountId(event.botAccountId().toString());
          }
          if (event.botAccountName() != null) {
            builder.setBotAccountName(event.botAccountName());
          }
          if (event.scriptId() != null) {
            builder.setScriptId(event.scriptId().toString());
          }
          if (event.loggerName() != null) {
            builder.setLoggerName(event.loggerName());
          }
        }).build());
    }

    private void publish(EventEntry event) {
      synchronized (publishLock) {
        event = aggregate(event);
        events.add(event);

        for (var sender : subscribers.values()) {
          if (sender.eventPredicate().test(event)) {
            sender.sendMessage(event);
          }
        }
      }
    }

    private EventEntry aggregate(EventEntry event) {
      var timestamp = Timestamps.toMillis(event.getTimestamp());
      recentAggregates.entrySet().removeIf(entry -> timestamp - entry.getValue().lastTimestamp() > AGGREGATION_WINDOW_MS);

      var key = aggregateKey(event);
      var previous = recentAggregates.get(key);
      if (previous != null
        && timestamp >= previous.lastTimestamp()
        && timestamp - previous.lastTimestamp() <= AGGREGATION_WINDOW_MS) {
        event = event.toBuilder()
          .setId(previous.event().getId())
          .setRepeatCount(previous.event().getRepeatCount() + 1)
          .build();
      }

      recentAggregates.put(key, new AggregateState(event, timestamp));
      return event;
    }

    private static EventEntry.Builder baseBuilder(
      String id,
      long timestamp,
      EventSeverity severity,
      EventCategory category,
      EventType type,
      String summary,
      @Nullable String body,
      boolean personal
    ) {
      var builder = EventEntry.newBuilder()
        .setId(id)
        .setTimestamp(Timestamps.fromMillis(timestamp))
        .setSeverity(severity)
        .setCategory(category)
        .setType(type)
        .setSummary(summary)
        .setPersonal(personal)
        .setRepeatCount(1);
      if (body != null && !body.isBlank()) {
        builder.setBody(body);
      }
      return builder;
    }

    private static EventEntry.Builder let(EventEntry.Builder builder, Consumer<EventEntry.Builder> consumer) {
      consumer.accept(builder);
      return builder;
    }

    private static void applyUser(EventEntry.Builder builder, @Nullable SoulFireUser actor) {
      if (actor == null) {
        return;
      }

      builder.setUserId(actor.getUniqueId().toString());
      builder.setUserName(actor.getUsername());
    }

    private static EventSeverity toSeverity(Level level) {
      return switch (level) {
        case ERROR -> EventSeverity.EVENT_SEVERITY_ERROR;
        case WARN -> EventSeverity.EVENT_SEVERITY_WARN;
        default -> EventSeverity.EVENT_SEVERITY_INFO;
      };
    }

    private static String nextId() {
      return System.currentTimeMillis() + "-" + EVENT_COUNTER.getAndIncrement();
    }

    private static String stripAnsi(String input) {
      return ANSI_ESCAPE_PATTERN.matcher(input).replaceAll("");
    }

    private static String summarize(String input) {
      var normalized = input.replace("\r\n", "\n").replace('\r', '\n').trim();
      if (normalized.isBlank()) {
        return normalized;
      }

      var firstLine = normalized.lines()
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .findFirst()
        .orElse(normalized);
      return firstLine.length() > 220 ? firstLine.substring(0, 217) + "..." : firstLine;
    }

    private static EventSummary summarizeSystemMessage(String input) {
      var normalized = input.replace("\r\n", "\n").replace('\r', '\n').trim();
      if (normalized.isBlank()) {
        return new EventSummary("", null);
      }

      var firstLine = summarize(normalized);
      var colonIndex = firstLine.indexOf(':');
      if (colonIndex > 0) {
        var type = firstLine.substring(0, colonIndex).trim();
        var detail = firstLine.substring(colonIndex + 1).trim();
        if (!detail.isBlank() && (type.contains("Exception") || type.contains("Error"))) {
          var simpleType = type.substring(type.lastIndexOf('.') + 1);
          return new EventSummary(detail, simpleType);
        }
      }

      return new EventSummary(firstLine, null);
    }

    private static String aggregateKey(EventEntry event) {
      return String.join("\u001F",
        event.getPersonal() ? "1" : "0",
        event.getSeverity().name(),
        event.getCategory().name(),
        event.getType().name(),
        event.getSummary(),
        event.getBody(),
        event.getInstanceId(),
        event.getBotAccountId(),
        event.getScriptId(),
        event.getUserId(),
        event.getCommand(),
        event.getLoggerName());
    }

    private record MessageContext(
      @Nullable UUID instanceId,
      @Nullable String instanceName,
      @Nullable UUID botId,
      @Nullable String botName
    ) {}

    private record AggregateState(EventEntry event, long lastTimestamp) {}

    private record EventSummary(String summary, @Nullable String body) {}

    private record ConnectionMessageSender(
      StateHolder stateHolder,
      UUID userId,
      ServerCallStreamObserver<EventResponse> responseObserver,
      EventPredicate eventPredicate
    ) {
      private ConnectionMessageSender {
        var responseId = UUID.randomUUID();
        stateHolder.subscribers.put(responseId, this);
        responseObserver.setOnCancelHandler(() -> stateHolder.subscribers.remove(responseId));
        responseObserver.setOnCloseHandler(() -> stateHolder.subscribers.remove(responseId));
      }

      public void sendMessage(EventEntry event) {
        if (responseObserver.isCancelled()) {
          return;
        }

        responseObserver.onNext(EventResponse.newBuilder()
          .setEvent(event)
          .build());
      }
    }
  }
}
