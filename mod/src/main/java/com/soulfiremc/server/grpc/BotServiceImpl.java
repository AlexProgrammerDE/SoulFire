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
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.settings.lib.BotSettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class BotServiceImpl extends BotServiceGrpc.BotServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void getBotInfo(BotInfoRequest request, StreamObserver<BotInfoResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      var account = instance.settingsSource().accounts().get(botId);
      if (account == null) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Bot '%s' not found in instance '%s'".formatted(botId, instanceId)));
      }

      var settingsStem = account.settingsStem() == null ? BotSettingsImpl.Stem.EMPTY : account.settingsStem();
      var botInfoResponseBuilder = BotInfoResponse.newBuilder()
        .setConfig(settingsStem.toProto());
      var activeBot = instance.botConnections().get(botId);
      if (activeBot != null) {
        var minecraft = activeBot.minecraft();
        var player = minecraft.player;
        if (player != null) {
          botInfoResponseBuilder.setLiveState(BotLiveState.newBuilder()
            .setX(player.getX())
            .setY(player.getY())
            .setZ(player.getZ())
            .setXRot(player.getXRot())
            .setYRot(player.getYRot())
            .build());
        }
      }

      responseObserver.onNext(botInfoResponseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateBotConfig(BotUpdateConfigRequest request, StreamObserver<BotUpdateConfigResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        instanceEntity.settings(instanceEntity.settings().withAccounts(instanceEntity.settings().accounts().stream()
          .map(minecraftAccount -> {
            if (minecraftAccount.profileId().equals(botId)) {
              return minecraftAccount.withSettingsStem(BotSettingsImpl.Stem.fromProto(request.getConfig()));
            } else {
              return minecraftAccount;
            }
          })
          .toList()));

        session.merge(instanceEntity);
      });

      responseObserver.onNext(BotUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot config", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateBotConfigEntry(BotUpdateConfigEntryRequest request, StreamObserver<BotUpdateConfigEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        instanceEntity.settings(instanceEntity.settings().withAccounts(instanceEntity.settings().accounts().stream()
          .map(minecraftAccount -> {
            if (minecraftAccount.profileId().equals(botId)) {
              var currentStem = minecraftAccount.settingsStem() == null ? BotSettingsImpl.Stem.EMPTY : minecraftAccount.settingsStem();
              var newSettings = SettingsSource.Stem.withUpdatedEntry(
                currentStem.settings(),
                request.getNamespace(),
                request.getKey(),
                SettingsSource.Stem.valueToJsonElement(request.getValue())
              );
              return minecraftAccount.withSettingsStem(currentStem.withSettings(newSettings));
            } else {
              return minecraftAccount;
            }
          })
          .toList()));

        session.merge(instanceEntity);
      });

      responseObserver.onNext(BotUpdateConfigEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot config entry", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
