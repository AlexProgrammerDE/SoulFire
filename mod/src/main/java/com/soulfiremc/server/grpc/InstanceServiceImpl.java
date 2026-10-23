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
import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.database.AuditLogType;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SocketAddressHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class InstanceServiceImpl extends InstanceServiceGrpc.InstanceServiceImplBase {
  private final SoulFireServer soulFireServer;
  private final Object provisioningLock = new Object();

  @Override
  public void getOrCreateInstance(InstanceGetOrCreateRequest request, StreamObserver<InstanceGetOrCreateResponse> responseObserver) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    var name = InstanceNameValidation.requireName(request.getName());
    var server = request.hasServer() ? SdkProvisioning.requireServer(request.getServer()) : null;
    InstanceGetOrCreateResponse response;
    synchronized (provisioningLock) {
      var records = soulFireServer.dsl().selectFrom(Tables.INSTANCES)
        .where(Tables.INSTANCES.OWNER_ID.eq(user.getUniqueId().toString()))
        .and(Tables.INSTANCES.FRIENDLY_NAME.eq(name)).fetch();
      if (records.size() > 1) {
        throw Status.FAILED_PRECONDITION.withDescription("More than one instance has this name. Select an instance by ID or rename it.").asRuntimeException();
      }
      if (records.isEmpty()) {
        user.hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_INSTANCE));
        var settings = server == null ? InstanceSettingsImpl.Stem.EMPTY : SdkProvisioning.instanceSettings(server);
        var id = soulFireServer.createInstance(name, user, settings);
        response = InstanceGetOrCreateResponse.newBuilder().setId(id.toString()).setCreated(true).build();
      } else {
        var record = records.getFirst();
        user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, UUID.fromString(record.getId())));
        if (server != null) {
          SdkProvisioning.requireServerMatch(parseSettings(record.getSettings()), server);
        }
        response = InstanceGetOrCreateResponse.newBuilder().setId(record.getId()).build();
      }
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getOrCreateBot(InstanceGetOrCreateBotRequest request, StreamObserver<InstanceGetOrCreateBotResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));
    // Ownership checks need a connection before the settings transaction acquires one.
    var canUpdateConfig = user.hasPermission(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));
    SdkProvisioning.AccountResult result;
    synchronized (provisioningLock) {
      result = soulFireServer.dsl().transactionResult(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(request.getId())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance does not exist").asRuntimeException();
        }
        var provisioned = SdkProvisioning.account(parseSettings(record.getSettings()), request);
        if (provisioned.created()) {
          if (!canUpdateConfig) {
            throw Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource").asRuntimeException();
          }
          ctx.update(Tables.INSTANCES)
            .set(Tables.INSTANCES.SETTINGS, serializeSettings(provisioned.settings()))
            .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(Tables.INSTANCES.ID.eq(request.getId())).execute();
        }
        return provisioned;
      });
      if (result.created()) {
        refreshRuntimeSettings(instanceId);
      }
    }
    responseObserver.onNext(InstanceGetOrCreateBotResponse.newBuilder()
      .setBotId(result.account().profileId().toString()).setCreated(result.created()).build());
    responseObserver.onCompleted();
  }

  private static Collection<InstancePermissionState> instancePermissionStates(Predicate<InstancePermission> permissions) {
    return Arrays.stream(InstancePermission.values())
      .filter(permission -> permission != InstancePermission.UNRECOGNIZED)
      .map(permission -> InstancePermissionState.newBuilder()
        .setInstancePermission(permission)
        .setGranted(permissions.test(permission))
        .build())
      .toList();
  }

  private InstanceSettingsImpl.Stem parseSettings(String settingsJson) {
    return InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(settingsJson, JsonElement.class));
  }

  private String serializeSettings(InstanceSettingsImpl.Stem stem) {
    return GsonInstance.CONFIG_GSON.toJson(stem.serializeToTree());
  }

  private void refreshRuntimeSettings(UUID instanceId) {
    soulFireServer.getInstance(instanceId).ifPresent(instance -> {
      instance.invalidateSettingsCache();
      instance.botStateManager().syncConfiguredAccounts();
    });
  }

  @Override
  public void createInstance(InstanceCreateRequest request, StreamObserver<InstanceCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_INSTANCE));

    var name = InstanceNameValidation.requireName(request.getFriendlyName());
    try {
      var id = soulFireServer.createInstance(name, ServerRPCConstants.USER_CONTEXT_KEY.get());
      responseObserver.onNext(InstanceCreateResponse.newBuilder().setId(id.toString()).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error creating instance", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void duplicateInstance(InstanceDuplicateRequest request, StreamObserver<InstanceDuplicateResponse> responseObserver) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    user.hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_INSTANCE));
    UUID sourceId;
    try {
      sourceId = UUID.fromString(request.getId());
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription("Source instance ID must be a UUID.").withCause(e).asRuntimeException();
    }
    user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, sourceId));
    var name = InstanceNameValidation.requireName(request.getFriendlyName());

    try {
      var source = soulFireServer.dsl().selectFrom(Tables.INSTANCES)
        .where(Tables.INSTANCES.ID.eq(sourceId.toString())).fetchOne();
      if (source == null) {
        throw Status.NOT_FOUND.withDescription("Source instance '%s' not found".formatted(sourceId)).asRuntimeException();
      }
      var id = soulFireServer.createInstance(name, user, parseSettings(source.getSettings()), source.getIcon());
      responseObserver.onNext(InstanceDuplicateResponse.newBuilder().setId(id.toString()).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error duplicating instance", t);
      throw Status.INTERNAL.withDescription("Failed to duplicate instance.").withCause(t).asRuntimeException();
    }
  }

  @Override
  public void deleteInstance(InstanceDeleteRequest request, StreamObserver<InstanceDeleteResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DELETE_INSTANCE, instanceId));

    try {
      var optionalDeletion = soulFireServer.deleteInstance(instanceId);
      if (optionalDeletion.isEmpty()) {
        throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
      }

      optionalDeletion.get().join();
      responseObserver.onNext(InstanceDeleteResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error deleting instance", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void listInstances(InstanceListRequest request, StreamObserver<InstanceListResponse> responseObserver) {
    try {
      var dsl = soulFireServer.dsl();
      var records = dsl.selectFrom(Tables.INSTANCES).fetch();
      var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
      var response = InstanceListResponse.newBuilder();
      for (var record : records) {
        var id = UUID.fromString(record.getId());
        var permissions = user.instancePermissionChecker(id);
        if (!permissions.test(InstancePermission.READ_INSTANCE)) continue;
        var instance = soulFireServer.getInstance(id);
        if (instance.isEmpty()) continue;
        response.addInstances(InstanceListResponse.Instance.newBuilder()
          .setId(record.getId())
          .setFriendlyName(record.getFriendlyName())
          .setIcon(record.getIcon())
          .setBotSummary(instance.get().botStateManager().summary())
          .addAllInstancePermissions(instancePermissionStates(permissions)));
      }
      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error listing instance", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void getInstanceInfo(InstanceInfoRequest request, StreamObserver<InstanceInfoResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    user.hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
      if (record == null) {
        throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
      }

      var lastModified = record.getUpdatedAt().toInstant(ZoneOffset.UTC);
      var lastModifiedProto = Timestamps.fromMillis(lastModified.toEpochMilli());
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException());
      var permissions = instancePermissionStates(user.instancePermissionChecker(instanceId));
      var botSummary = instance.botStateManager().summary();

      // Check if-modified-since
      if (request.hasIfModifiedSince()) {
        var ifModifiedSince = Instant.ofEpochSecond(
          request.getIfModifiedSince().getSeconds(),
          request.getIfModifiedSince().getNanos()
        );
        if (!lastModified.isAfter(ifModifiedSince)) {
          // Not modified since the given timestamp
          responseObserver.onNext(InstanceInfoResponse.newBuilder()
            .setNotModified(InstanceNotModified.newBuilder()
              .setLastModified(lastModifiedProto)
              .addAllInstancePermissions(permissions)
              .setBotSummary(botSummary)
              .build())
            .build());
          responseObserver.onCompleted();
          return;
        }
      }

      var registry = instance.instanceSettingsPageRegistry();
      var settings = parseSettings(record.getSettings());
      responseObserver.onNext(InstanceInfoResponse.newBuilder()
        .setInfo(InstanceInfo.newBuilder()
          .setFriendlyName(record.getFriendlyName())
          .setIcon(record.getIcon())
          .setConfig(settings.toProto())
          .setBotSummary(botSummary)
          .addAllInstancePermissions(permissions)
          .addAllSettingsDefinitions(registry.exportSettingsDefinitions())
          .addAllInstanceSettings(registry.exportSettingsPages())
          .addAllPlugins(registry.exportRegisteredPlugins())
          .setLastModified(lastModifiedProto)
          .build())
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateInstanceMeta(InstanceUpdateMetaRequest request, StreamObserver<InstanceUpdateMetaResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_META, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var updateStep = switch (request.getMetaCase()) {
          case FRIENDLY_NAME -> ctx.update(Tables.INSTANCES).set(Tables.INSTANCES.FRIENDLY_NAME, InstanceNameValidation.requireName(request.getFriendlyName()));
          case ICON -> ctx.update(Tables.INSTANCES).set(Tables.INSTANCES.ICON, request.getIcon());
          case META_NOT_SET -> throw Status.INVALID_ARGUMENT.withDescription("Choose an instance metadata field to update.").asRuntimeException();
        };

        updateStep
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      soulFireServer.getInstance(instanceId).ifPresent(InstanceManager::refreshFriendlyName);
      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceUpdateMetaResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating instance meta", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateInstanceConfig(InstanceUpdateConfigRequest request, StreamObserver<InstanceUpdateConfigResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> Status.NOT_FOUND
          .withDescription("Instance '%s' not found".formatted(instanceId))
          .asRuntimeException());
      var newSettings = InstanceSettingsImpl.Stem.fromProto(request.getConfig());
      var newAccountIds = newSettings.accounts().stream()
        .map(MinecraftAccount::profileId)
        .collect(Collectors.toSet());
      var removedAccountIds = instance.settingsSource().accounts().keySet().stream()
        .filter(profileId -> !newAccountIds.contains(profileId))
        .toList();
      instance.botStateManager().removeAccounts(removedAccountIds).join();

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(newSettings))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating instance config", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateInstanceConfigEntry(InstanceUpdateConfigEntryRequest request, StreamObserver<InstanceUpdateConfigEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newSettings = SettingsSource.Stem.withUpdatedEntry(
          currentSettings.settings(),
          request.getNamespace(),
          request.getKey(),
          SettingsSource.Stem.valueToJsonElement(request.getValue())
        );
        var updatedStem = currentSettings.withSettings(newSettings);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceUpdateConfigEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating instance config entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void addInstanceAccount(InstanceAddAccountRequest request, StreamObserver<InstanceAddAccountResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newAccounts = new ArrayList<>(currentSettings.accounts());
        newAccounts.add(MinecraftAccount.fromProto(request.getAccount()));
        var updatedStem = currentSettings.withAccounts(newAccounts);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceAddAccountResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error adding instance account", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void removeInstanceAccount(InstanceRemoveAccountRequest request, StreamObserver<InstanceRemoveAccountResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    var profileId = UUID.fromString(request.getProfileId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> Status.NOT_FOUND
          .withDescription("Instance '%s' not found".formatted(instanceId))
          .asRuntimeException());
      instance.botStateManager().removeAccounts(List.of(profileId)).join();

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newAccounts = currentSettings.accounts().stream()
          .filter(account -> !account.profileId().equals(profileId))
          .toList();
        var updatedStem = currentSettings.withAccounts(newAccounts);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      instance.invalidateSettingsCache();
      instance.botStateManager().syncConfiguredAccounts();
      responseObserver.onNext(InstanceRemoveAccountResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error removing instance account", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateInstanceAccount(InstanceUpdateAccountRequest request, StreamObserver<InstanceUpdateAccountResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var updatedAccount = MinecraftAccount.fromProto(request.getAccount());
        var currentSettings = parseSettings(record.getSettings());
        var newAccounts = currentSettings.accounts().stream()
          .map(account -> account.profileId().equals(updatedAccount.profileId()) ? updatedAccount : account)
          .toList();
        var updatedStem = currentSettings.withAccounts(newAccounts);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceUpdateAccountResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating instance account", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void addInstanceAccountsBatch(InstanceAddAccountsBatchRequest request, StreamObserver<InstanceAddAccountsBatchResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newAccounts = new ArrayList<>(currentSettings.accounts());
        for (var accountProto : request.getAccountsList()) {
          newAccounts.add(MinecraftAccount.fromProto(accountProto));
        }
        var updatedStem = currentSettings.withAccounts(newAccounts);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceAddAccountsBatchResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error adding instance accounts batch", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void replaceInstanceAccounts(InstanceReplaceAccountsRequest request, StreamObserver<InstanceReplaceAccountsResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var accounts = request.getAccountsList().stream().map(MinecraftAccount::fromProto).toList();
      var accountIds = accounts.stream().map(MinecraftAccount::profileId).collect(Collectors.toSet());
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> Status.NOT_FOUND
          .withDescription("Instance '%s' not found".formatted(instanceId))
          .asRuntimeException());
      var removedAccountIds = soulFireServer.dsl().transactionResult(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var removed = currentSettings.accounts().stream()
          .map(MinecraftAccount::profileId)
          .filter(profileId -> !accountIds.contains(profileId))
          .toList();
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(currentSettings.withAccounts(accounts)))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
        return removed;
      });

      instance.invalidateSettingsCache();
      instance.botStateManager().removeAccounts(removedAccountIds).join();
      instance.botStateManager().syncConfiguredAccounts();
      responseObserver.onNext(InstanceReplaceAccountsResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    } catch (Throwable t) {
      log.error("Error replacing instance accounts", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void removeInstanceAccountsBatch(InstanceRemoveAccountsBatchRequest request, StreamObserver<InstanceRemoveAccountsBatchResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var profileIds = request.getProfileIdsList().stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
      var instance = soulFireServer.getInstance(instanceId)
        .orElseThrow(() -> Status.NOT_FOUND
          .withDescription("Instance '%s' not found".formatted(instanceId))
          .asRuntimeException());
      instance.botStateManager().removeAccounts(profileIds).join();

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newAccounts = currentSettings.accounts().stream()
          .filter(account -> !profileIds.contains(account.profileId()))
          .toList();
        var updatedStem = currentSettings.withAccounts(newAccounts);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      instance.invalidateSettingsCache();
      instance.botStateManager().syncConfiguredAccounts();
      responseObserver.onNext(InstanceRemoveAccountsBatchResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error removing instance accounts batch", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void addInstanceProxy(InstanceAddProxyRequest request, StreamObserver<InstanceAddProxyResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newProxies = new ArrayList<>(currentSettings.proxies());
        newProxies.add(SFProxy.fromProto(request.getProxy()));
        var updatedStem = currentSettings.withProxies(newProxies);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceAddProxyResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error adding instance proxy", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void removeInstanceProxy(InstanceRemoveProxyRequest request, StreamObserver<InstanceRemoveProxyResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    var index = request.getIndex();
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newProxies = new ArrayList<>(currentSettings.proxies());
        if (index < 0 || index >= newProxies.size()) {
          throw Status.INVALID_ARGUMENT.withDescription("Proxy index '%d' out of bounds".formatted(index)).asRuntimeException();
        }
        newProxies.remove(index);
        var updatedStem = currentSettings.withProxies(newProxies);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceRemoveProxyResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error removing instance proxy", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateInstanceProxy(InstanceUpdateProxyRequest request, StreamObserver<InstanceUpdateProxyResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    var index = request.getIndex();
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newProxies = new ArrayList<>(currentSettings.proxies());
        if (index < 0 || index >= newProxies.size()) {
          throw Status.INVALID_ARGUMENT.withDescription("Proxy index '%d' out of bounds".formatted(index)).asRuntimeException();
        }
        newProxies.set(index, SFProxy.fromProto(request.getProxy()));
        var updatedStem = currentSettings.withProxies(newProxies);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceUpdateProxyResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error updating instance proxy", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void addInstanceProxiesBatch(InstanceAddProxiesBatchRequest request, StreamObserver<InstanceAddProxiesBatchResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newProxies = new ArrayList<>(currentSettings.proxies());
        for (var proxyProto : request.getProxiesList()) {
          newProxies.add(SFProxy.fromProto(proxyProto));
        }
        var updatedStem = currentSettings.withProxies(newProxies);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceAddProxiesBatchResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error adding instance proxies batch", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void removeInstanceProxyEntries(InstanceRemoveProxyEntriesRequest request, StreamObserver<InstanceRemoveProxyEntriesResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var selected = request.getProxiesList().stream().map(SFProxy::fromProto).toList();
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var remainingToRemove = new HashMap<SFProxy, Integer>();
        for (var proxy : selected) {
          remainingToRemove.merge(proxy, 1, Integer::sum);
        }
        var newProxies = new ArrayList<SFProxy>();
        for (var proxy : currentSettings.proxies()) {
          var count = remainingToRemove.getOrDefault(proxy, 0);
          if (count == 0) {
            newProxies.add(proxy);
          } else if (count == 1) {
            remainingToRemove.remove(proxy);
          } else {
            remainingToRemove.put(proxy, count - 1);
          }
        }

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(currentSettings.withProxies(newProxies)))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceRemoveProxyEntriesResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    } catch (Throwable t) {
      log.error("Error removing instance proxy entries", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void removeInstanceProxiesBatch(InstanceRemoveProxiesBatchRequest request, StreamObserver<InstanceRemoveProxiesBatchResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      var addressesToRemove = new HashSet<>(request.getAddressesList());

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newProxies = currentSettings.proxies().stream()
          .filter(proxy -> !addressesToRemove.contains(SocketAddressHelper.serialize(proxy.address())))
          .toList();
        var updatedStem = currentSettings.withProxies(newProxies);

        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      refreshRuntimeSettings(instanceId);
      responseObserver.onNext(InstanceRemoveProxiesBatchResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error removing instance proxies batch", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void getAuditLog(InstanceAuditLogRequest request, StreamObserver<InstanceAuditLogResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE_AUDIT_LOGS, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      var instanceExists = dsl.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(instanceId.toString()));
      if (!instanceExists) {
        throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
      }

      var auditLogs = dsl.selectFrom(Tables.INSTANCE_AUDIT_LOGS)
        .where(Tables.INSTANCE_AUDIT_LOGS.INSTANCE_ID.eq(instanceId.toString()))
        .orderBy(Tables.INSTANCE_AUDIT_LOGS.CREATED_AT.desc())
        .fetch();

      var responseBuilder = InstanceAuditLogResponse.newBuilder();
      for (var auditLog : auditLogs) {
        var userRecord = dsl.selectFrom(Tables.USERS)
          .where(Tables.USERS.ID.eq(auditLog.getUserId()))
          .fetchOne();

        var userBuilder = InstanceUser.newBuilder()
          .setId(auditLog.getUserId());
        if (userRecord != null) {
          userBuilder.setUsername(userRecord.getUsername())
            .setEmail(userRecord.getEmail());
        }

        var auditLogType = AuditLogType.valueOf(auditLog.getType());
        responseBuilder.addEntry(InstanceAuditLogResponse.AuditLogEntry.newBuilder()
          .setId(auditLog.getId())
          .setUser(userBuilder.build())
          .setType(switch (auditLogType) {
            case EXECUTE_COMMAND -> InstanceAuditLogResponse.AuditLogEntryType.EXECUTE_COMMAND;
            case BOT_DESIRED_STATE_CHANGE -> InstanceAuditLogResponse.AuditLogEntryType.BOT_DESIRED_STATE_CHANGE;
            case BOT_RESTART -> InstanceAuditLogResponse.AuditLogEntryType.BOT_RESTART;
            case PLUGIN_RPC -> InstanceAuditLogResponse.AuditLogEntryType.PLUGIN_RPC;
          })
          .setTimestamp(Timestamps.fromMillis(auditLog.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
          .setData(auditLog.getData() != null ? auditLog.getData() : "")
          .build());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting audit logs", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void getAccountMetadata(GetAccountMetadataRequest request, StreamObserver<GetAccountMetadataResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var accountId = UUID.fromString(request.getAccountId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
      if (record == null) {
        throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
      }

      var settings = parseSettings(record.getSettings());
      var account = requireAccount(settings, instanceId, accountId);

      responseObserver.onNext(GetAccountMetadataResponse.newBuilder()
        .addAllMetadata(SettingsSource.Stem.mapToSettingsNamespaceProto(account.persistentMetadata()))
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting account metadata", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void setAccountMetadataEntry(SetAccountMetadataEntryRequest request, StreamObserver<SetAccountMetadataEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var accountId = UUID.fromString(request.getAccountId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      validateMetadataIdentifier(request.getNamespace(), request.getKey());
      var jsonValue = SettingsSource.Stem.valueToJsonElement(request.getValue());

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        requireAccount(currentSettings, instanceId, accountId);
        var newAccounts = currentSettings.accounts().stream()
          .map(account -> {
            if (account.profileId().equals(accountId)) {
              var newMetadata = SettingsSource.Stem.withUpdatedEntry(
                account.persistentMetadata(),
                request.getNamespace(),
                request.getKey(),
                jsonValue);
              return account.withPersistentMetadata(newMetadata);
            }
            return account;
          })
          .toList();

        var updatedStem = currentSettings.withAccounts(newAccounts);
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      // Also update the live bot's persistent metadata if it's connected
      soulFireServer.getInstance(instanceId).ifPresent(instance -> {
        instance.invalidateSettingsCache();
        var bot = instance.botConnections().get(accountId);
        if (bot != null) {
          bot.persistentMetadata().setPersisted(request.getNamespace(), request.getKey(), jsonValue);
        }
      });

      responseObserver.onNext(SetAccountMetadataEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error setting account metadata entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void deleteAccountMetadataEntry(DeleteAccountMetadataEntryRequest request, StreamObserver<DeleteAccountMetadataEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var accountId = UUID.fromString(request.getAccountId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      validateMetadataIdentifier(request.getNamespace(), request.getKey());
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        requireAccount(currentSettings, instanceId, accountId);
        var newAccounts = currentSettings.accounts().stream()
          .map(account -> {
            if (account.profileId().equals(accountId)) {
              var newMetadata = SettingsSource.Stem.withDeletedEntry(
                account.persistentMetadata(),
                request.getNamespace(),
                request.getKey());
              return account.withPersistentMetadata(newMetadata);
            }
            return account;
          })
          .toList();

        var updatedStem = currentSettings.withAccounts(newAccounts);
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      // Also update the live bot's persistent metadata if it's connected
      soulFireServer.getInstance(instanceId).ifPresent(instance -> {
        instance.invalidateSettingsCache();
        var bot = instance.botConnections().get(accountId);
        if (bot != null) {
          bot.persistentMetadata().removePersisted(request.getNamespace(), request.getKey());
        }
      });

      responseObserver.onNext(DeleteAccountMetadataEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error deleting account metadata entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void getInstanceMetadata(GetInstanceMetadataRequest request, StreamObserver<GetInstanceMetadataResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      var dsl = soulFireServer.dsl();
      var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
      if (record == null) {
        throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
      }

      var settings = parseSettings(record.getSettings());

      responseObserver.onNext(GetInstanceMetadataResponse.newBuilder()
        .addAllMetadata(SettingsSource.Stem.mapToSettingsNamespaceProto(settings.persistentMetadata()))
        .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error getting instance metadata", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void setInstanceMetadataEntry(SetInstanceMetadataEntryRequest request, StreamObserver<SetInstanceMetadataEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_META, instanceId));

    try {
      validateMetadataIdentifier(request.getNamespace(), request.getKey());
      var jsonValue = SettingsSource.Stem.valueToJsonElement(request.getValue());

      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newMetadata = SettingsSource.Stem.withUpdatedEntry(
          currentSettings.persistentMetadata(),
          request.getNamespace(),
          request.getKey(),
          jsonValue);

        var updatedStem = currentSettings.withPersistentMetadata(newMetadata);
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      // Also update the live instance's persistent metadata if loaded
      soulFireServer.getInstance(instanceId).ifPresent(instance -> {
        instance.invalidateSettingsCache();
        instance.persistentMetadata().setPersisted(request.getNamespace(), request.getKey(), jsonValue);
      });

      responseObserver.onNext(SetInstanceMetadataEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error setting instance metadata entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void deleteInstanceMetadataEntry(DeleteInstanceMetadataEntryRequest request, StreamObserver<DeleteInstanceMetadataEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_META, instanceId));

    try {
      validateMetadataIdentifier(request.getNamespace(), request.getKey());
      var dsl = soulFireServer.dsl();
      dsl.transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
        if (record == null) {
          throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
        }

        var currentSettings = parseSettings(record.getSettings());
        var newMetadata = SettingsSource.Stem.withDeletedEntry(
          currentSettings.persistentMetadata(),
          request.getNamespace(),
          request.getKey());

        var updatedStem = currentSettings.withPersistentMetadata(newMetadata);
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, serializeSettings(updatedStem))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
          .execute();
      });

      // Also update the live instance's persistent metadata if loaded
      soulFireServer.getInstance(instanceId).ifPresent(instance -> {
        instance.invalidateSettingsCache();
        instance.persistentMetadata().removePersisted(request.getNamespace(), request.getKey());
      });

      responseObserver.onNext(DeleteInstanceMetadataEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Throwable t) {
      log.error("Error deleting instance metadata entry", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  private static void validateMetadataIdentifier(String namespace, String key) {
    if (namespace.isEmpty() || !Key.parseableNamespace(namespace)) {
      throw Status.INVALID_ARGUMENT.withDescription("Metadata namespace must contain only lowercase letters, digits, underscores, periods, or hyphens.").asRuntimeException();
    }
    if (key.isEmpty() || !Key.parseableValue(key)) {
      throw Status.INVALID_ARGUMENT.withDescription("Metadata key must contain only lowercase letters, digits, underscores, periods, hyphens, or slashes.").asRuntimeException();
    }
  }

  private static MinecraftAccount requireAccount(InstanceSettingsImpl.Stem settings, UUID instanceId, UUID accountId) {
    return settings.accounts().stream()
      .filter(account -> account.profileId().equals(accountId))
      .findFirst()
      .orElseThrow(() -> Status.NOT_FOUND
        .withDescription("Account '%s' not found in instance '%s'".formatted(accountId, instanceId)).asRuntimeException());
  }
}
