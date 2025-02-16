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
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CancellationCollector;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MCAuthServiceImpl extends MCAuthServiceGrpc.MCAuthServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void loginCredentials(CredentialsAuthRequest request, StreamObserver<CredentialsAuthResponse> casted) {
    var responseObserver = (ServerCallStreamObserver<CredentialsAuthResponse>) casted;
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.AUTHENTICATE_MC_ACCOUNT, instanceId));

    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settings = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      instance.scheduler().runAsync(() -> {
        var service = MCAuthService.convertService(request.getService());
        var results = SFHelpers.maxFutures(settings.get(AccountSettings.ACCOUNT_IMPORT_CONCURRENCY), request.getPayloadList(), payload ->
              cancellationCollector.add(service.createDataAndLogin(
                  payload,
                  settings.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH) ? SFHelpers.getRandomEntry(settings.proxies()) : null,
                  instance.scheduler()
                ))
                .thenApply(MinecraftAccount::toProto)
                .exceptionally(t -> {
                  log.error("Error authenticating account", t);
                  return null;
                }), result -> {
              if (responseObserver.isCancelled()) {
                return;
              }

              if (result == null) {
                responseObserver.onNext(CredentialsAuthResponse.newBuilder()
                  .setOneFailure(CredentialsAuthOneFailure.newBuilder()
                    .build())
                  .build());
              } else {
                responseObserver.onNext(CredentialsAuthResponse.newBuilder()
                  .setOneSuccess(CredentialsAuthOneSuccess.newBuilder()
                    .build())
                  .build());
              }
            },
            cancellationCollector)
          .stream()
          .filter(Objects::nonNull)
          .toList();

        if (responseObserver.isCancelled()) {
          return;
        }

        responseObserver.onNext(CredentialsAuthResponse.newBuilder()
          .setFullList(CredentialsAuthFullList.newBuilder()
            .addAllAccount(results)
            .build())
          .build());
        responseObserver.onCompleted();
      });
    } catch (Throwable t) {
      log.error("Error authenticating account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void loginDeviceCode(DeviceCodeAuthRequest request, StreamObserver<DeviceCodeAuthResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.AUTHENTICATE_MC_ACCOUNT, instanceId));

    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settings = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    var service = MCAuthService.convertService(request.getService());
    cancellationCollector.add(service.createDataAndLogin(
        deviceCode ->
          responseObserver.onNext(DeviceCodeAuthResponse.newBuilder()
            .setDeviceCode(
              DeviceCode.newBuilder()
                .setDeviceCode(deviceCode.getDeviceCode())
                .setUserCode(deviceCode.getUserCode())
                .setVerificationUri(deviceCode.getVerificationUri())
                .setDirectVerificationUri(deviceCode.getDirectVerificationUri())
                .build()
            ).build()
          ),
        settings.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH) ? SFHelpers.getRandomEntry(settings.proxies()) : null,
        instance.scheduler()
      ))
      .whenComplete((account, t) -> {
        if (t != null) {
          log.error("Error authenticating account", t);
          responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t)));
        } else {
          responseObserver.onNext(DeviceCodeAuthResponse.newBuilder().setAccount(account.toProto()).build());
          responseObserver.onCompleted();
        }
      });
  }

  @Override
  public void refresh(RefreshRequest request, StreamObserver<RefreshResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.AUTHENTICATE_MC_ACCOUNT, instanceId));

    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settings = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      var receivedAccount = MinecraftAccount.fromProto(request.getAccount());
      var service = MCAuthService.convertService(request.getAccount().getType());
      var account = cancellationCollector.add(service.refresh(
        receivedAccount,
        settings.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH) ? SFHelpers.getRandomEntry(settings.proxies()) : null,
        instance.scheduler()
      )).join();

      responseObserver.onNext(RefreshResponse.newBuilder().setAccount(account.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error refreshing account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
