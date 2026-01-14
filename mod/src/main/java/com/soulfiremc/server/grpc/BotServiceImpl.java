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
import com.soulfiremc.server.bot.BotEntity;
import com.soulfiremc.server.bot.BotStateExtractor;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class BotServiceImpl extends BotServiceGrpc.BotServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void createBot(CreateBotRequest request, StreamObserver<CreateBotResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CREATE_BOT, instanceId));

    try {
      var bot = new BotEntity(
        UUID.randomUUID(),
        UUID.fromString(request.getAccountId()),
        request.hasProxyId() ? UUID.fromString(request.getProxyId()) : null,
        request.hasInitialMetadata()
          ? new ConcurrentHashMap<>(request.getInitialMetadata().getEntriesMap())
          : new ConcurrentHashMap<>(),
        request.getEnabled(),
        request.hasDisplayName() ? request.getDisplayName() : null
      );

      // Persist
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instance = session.find(InstanceEntity.class, instanceId);
        if (instance == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found"));
        }
        var bots = new ArrayList<>(instance.settings().bots());
        bots.add(bot);
        instance.settings(instance.settings().withBots(bots));
        session.merge(instance);
      });

      responseObserver.onNext(CreateBotResponse.newBuilder().setBot(bot.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error creating bot", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deleteBot(DeleteBotRequest request, StreamObserver<DeleteBotResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DELETE_BOT, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instance = session.find(InstanceEntity.class, instanceId);
        if (instance == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found"));
        }
        instance.settings(instance.settings().withRemovedBot(botId));
        session.merge(instance);
      });

      responseObserver.onNext(DeleteBotResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting bot", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateBot(UpdateBotRequest request, StreamObserver<UpdateBotResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT, instanceId));

    try {
      var updatedBot = soulFireServer.sessionFactory().fromTransaction(session -> {
        var instance = session.find(InstanceEntity.class, instanceId);
        if (instance == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found"));
        }

        var bot = instance.settings().getBotById(botId)
          .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Bot not found")));

        var newBot = switch (request.getUpdateCase()) {
          case ACCOUNT_ID -> bot.withAccountId(UUID.fromString(request.getAccountId()));
          case PROXY_ID -> bot.withProxyId(
            request.getProxyId().hasValue() ? UUID.fromString(request.getProxyId().getValue()) : null
          );
          case DISPLAY_NAME -> bot.withDisplayName(request.getDisplayName());
          case ENABLED -> bot.withEnabled(request.getEnabled());
          case UPDATE_NOT_SET -> throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("No update specified"));
        };

        instance.settings(instance.settings().withUpdatedBot(newBot));
        session.merge(instance);
        return newBot;
      });

      responseObserver.onNext(UpdateBotResponse.newBuilder().setBot(updatedBot.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void listBots(ListBotsRequest request, StreamObserver<ListBotsResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      var settings = instance.settingsSource();
      var botInfos = new ArrayList<BotInfo>();

      for (var bot : settings.bots()) {
        var builder = BotInfo.newBuilder()
          .setConfig(bot.toProto())
          .setAccountName(settings.getAccountById(bot.accountId())
            .map(a -> a.lastKnownName())
            .orElse("Unknown"));

        if (bot.proxyId() != null) {
          settings.getProxyById(bot.proxyId())
            .ifPresent(p -> builder.setProxyAddress(p.address().toString()));
        }

        if (request.getIncludeRuntimeState()) {
          var connection = instance.getBotConnection(bot.id()).orElse(null);
          if (connection != null) {
            builder.setRuntime(BotStateExtractor.extractRuntimeState(
              connection,
              request.getIncludeInventory()
            ));
          } else {
            builder.setRuntime(BotRuntimeState.newBuilder()
              .setBotId(bot.id().toString())
              .setConnectionState(BotConnectionState.BOT_OFFLINE)
              .setStateTimestampMs(System.currentTimeMillis())
              .build());
          }
        }

        botInfos.add(builder.build());
      }

      responseObserver.onNext(ListBotsResponse.newBuilder().addAllBots(botInfos).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing bots", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getBot(GetBotRequest request, StreamObserver<GetBotResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      var settings = instance.settingsSource();
      var bot = settings.getBotById(botId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Bot not found")));

      var builder = BotInfo.newBuilder()
        .setConfig(bot.toProto())
        .setAccountName(settings.getAccountById(bot.accountId())
          .map(a -> a.lastKnownName())
          .orElse("Unknown"));

      if (bot.proxyId() != null) {
        settings.getProxyById(bot.proxyId())
          .ifPresent(p -> builder.setProxyAddress(p.address().toString()));
      }

      var connection = instance.getBotConnection(bot.id()).orElse(null);
      if (connection != null) {
        builder.setRuntime(BotStateExtractor.extractRuntimeState(
          connection,
          request.getIncludeInventory()
        ));
      } else {
        builder.setRuntime(BotRuntimeState.newBuilder()
          .setBotId(bot.id().toString())
          .setConnectionState(BotConnectionState.BOT_OFFLINE)
          .setStateTimestampMs(System.currentTimeMillis())
          .build());
      }

      responseObserver.onNext(GetBotResponse.newBuilder().setBot(builder.build()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting bot", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getBotMetadata(GetBotMetadataRequest request, StreamObserver<GetBotMetadataResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      var bot = instance.settingsSource().getBotById(botId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Bot not found")));

      var metadataBuilder = BotMetadata.newBuilder();
      var keys = request.getKeysList();
      if (keys.isEmpty()) {
        // Return all metadata
        metadataBuilder.putAllEntries(bot.metadata());
      } else {
        // Return only requested keys
        for (var key : keys) {
          var value = bot.getMetadata(key);
          if (value != null) {
            metadataBuilder.putEntries(key, value);
          }
        }
      }

      responseObserver.onNext(GetBotMetadataResponse.newBuilder().setMetadata(metadataBuilder.build()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting bot metadata", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateBotMetadata(UpdateBotMetadataRequest request, StreamObserver<UpdateBotMetadataResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_METADATA, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      instance.updateBotMetadata(botId, request.getMetadata().getEntriesMap(), request.getMerge());

      // Return updated metadata
      var updatedBot = instance.settingsSource().getBotById(botId).orElseThrow();
      responseObserver.onNext(UpdateBotMetadataResponse.newBuilder()
        .setMetadata(BotMetadata.newBuilder().putAllEntries(updatedBot.metadata()).build())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot metadata", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deleteBotMetadataKeys(DeleteBotMetadataKeysRequest request, StreamObserver<DeleteBotMetadataKeysResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_METADATA, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      instance.deleteBotMetadataKeys(botId, request.getKeysList());

      responseObserver.onNext(DeleteBotMetadataKeysResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting bot metadata keys", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void subscribeBotState(SubscribeBotStateRequest request, StreamObserver<BotRuntimeState> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.SUBSCRIBE_BOT_STATE, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance not found")));

      var botIds = request.getBotIdsList().stream().map(UUID::fromString).toList();
      var intervalMs = Math.max(100, request.getIntervalMs()); // Minimum 100ms

      instance.scheduler().scheduleWithFixedDelay(() -> {
        try {
          var botsToStream = botIds.isEmpty()
            ? instance.settingsSource().bots()
            : instance.settingsSource().bots().stream()
            .filter(b -> botIds.contains(b.id()))
            .toList();

          for (var bot : botsToStream) {
            var connection = instance.getBotConnection(bot.id()).orElse(null);
            var state = connection != null
              ? BotStateExtractor.extractRuntimeState(connection, request.getIncludeInventory())
              : BotRuntimeState.newBuilder()
              .setBotId(bot.id().toString())
              .setConnectionState(BotConnectionState.BOT_OFFLINE)
              .setStateTimestampMs(System.currentTimeMillis())
              .build();
            responseObserver.onNext(state);
          }
        } catch (Exception e) {
          log.error("Error streaming bot state", e);
          responseObserver.onError(e);
        }
      }, 0, intervalMs, TimeUnit.MILLISECONDS);

      // Note: The scheduled task will be cancelled when the gRPC context is cancelled
      // This is handled automatically by the gRPC framework

    } catch (Throwable t) {
      log.error("Error subscribing to bot state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
