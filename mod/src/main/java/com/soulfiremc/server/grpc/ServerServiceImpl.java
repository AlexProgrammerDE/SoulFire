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
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DSL;


@Slf4j
@RequiredArgsConstructor
public final class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void getServerInfo(ServerInfoRequest request, StreamObserver<ServerInfoResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_SERVER_CONFIG));

    try {
      var record = soulFireServer.dsl().selectFrom(Tables.SERVER_CONFIG).where(Tables.SERVER_CONFIG.ID.eq(1L)).fetchOne();
      ServerSettingsImpl.Stem config;
      if (record == null) {
        config = ServerSettingsImpl.Stem.EMPTY;
      } else {
        config = ServerSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      }

      var registry = soulFireServer.settingsPageRegistry();
      responseObserver.onNext(ServerInfoResponse.newBuilder()
        .setConfig(config.toProto())
        .addAllSettingsDefinitions(registry.exportSettingsDefinitions())
        .addAllServerSettings(registry.exportSettingsPages())
        .addAllPlugins(registry.exportRegisteredPlugins())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting server info", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateServerConfig(ServerUpdateConfigRequest request, StreamObserver<ServerUpdateConfigResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_SERVER_CONFIG));

    try {
      var newStem = ServerSettingsImpl.Stem.fromProto(request.getConfig());
      var settingsJson = GsonInstance.GSON.toJson(newStem.serializeToTree());

      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var existing = ctx.selectFrom(Tables.SERVER_CONFIG).where(Tables.SERVER_CONFIG.ID.eq(1L)).fetchOne();
        if (existing == null) {
          ctx.insertInto(Tables.SERVER_CONFIG)
            .set(Tables.SERVER_CONFIG.ID, 1L)
            .set(Tables.SERVER_CONFIG.SETTINGS, settingsJson)
            .set(Tables.SERVER_CONFIG.VERSION, 0L)
            .execute();
        } else {
          ctx.update(Tables.SERVER_CONFIG)
            .set(Tables.SERVER_CONFIG.SETTINGS, settingsJson)
            .where(Tables.SERVER_CONFIG.ID.eq(1L))
            .execute();
        }
      });

      soulFireServer.configUpdateHook();
      responseObserver.onNext(ServerUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating server config", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateServerConfigEntry(ServerUpdateConfigEntryRequest request, StreamObserver<ServerUpdateConfigEntryResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_SERVER_CONFIG));

    try {
      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var existing = ctx.selectFrom(Tables.SERVER_CONFIG).where(Tables.SERVER_CONFIG.ID.eq(1L)).fetchOne();
        ServerSettingsImpl.Stem currentConfig;
        if (existing == null) {
          currentConfig = ServerSettingsImpl.Stem.EMPTY;
        } else {
          currentConfig = ServerSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(existing.getSettings(), JsonElement.class));
        }

        var newSettings = SettingsSource.Stem.withUpdatedEntry(
          currentConfig.settings(),
          request.getNamespace(),
          request.getKey(),
          SettingsSource.Stem.valueToJsonElement(request.getValue())
        );
        var updatedStem = currentConfig.withSettings(newSettings);
        var settingsJson = GsonInstance.GSON.toJson(updatedStem.serializeToTree());

        if (existing == null) {
          ctx.insertInto(Tables.SERVER_CONFIG)
            .set(Tables.SERVER_CONFIG.ID, 1L)
            .set(Tables.SERVER_CONFIG.SETTINGS, settingsJson)
            .set(Tables.SERVER_CONFIG.VERSION, 0L)
            .execute();
        } else {
          ctx.update(Tables.SERVER_CONFIG)
            .set(Tables.SERVER_CONFIG.SETTINGS, settingsJson)
            .where(Tables.SERVER_CONFIG.ID.eq(1L))
            .execute();
        }
      });

      soulFireServer.configUpdateHook();
      responseObserver.onNext(ServerUpdateConfigEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating server config entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }
}
