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
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.user.Permissions;
import com.soulfiremc.server.util.SFHelpers;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MCAuthServiceImpl extends MCAuthServiceGrpc.MCAuthServiceImplBase {
  @Override
  public void loginCredentials(CredentialsAuthRequest request, StreamObserver<CredentialsAuthResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    try {
      var service = MCAuthService.convertService(request.getService());
      var proxy = RPCUtils.convertProxy(request::hasProxy, request::getProxy);
      var results = SFHelpers.maxFutures(request.getMaxConcurrency(), request.getPayloadList(), payload ->
        service.createDataAndLogin(payload, proxy).thenApply(MinecraftAccount::toProto));

      responseObserver.onNext(CredentialsAuthResponse.newBuilder().addAllAccount(results).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error authenticating account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void loginDeviceCode(DeviceCodeAuthRequest request, StreamObserver<DeviceCodeAuthResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    MCAuthService.convertService(request.getService()).createDataAndLogin(deviceCode ->
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
        RPCUtils.convertProxy(request::hasProxy, request::getProxy))
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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    try {
      var receivedAccount = MinecraftAccount.fromProto(request.getAccount());
      var account = MCAuthService.convertService(request.getAccount().getType()).refresh(receivedAccount,
        RPCUtils.convertProxy(request::hasProxy, request::getProxy)).join();

      responseObserver.onNext(RefreshResponse.newBuilder().setAccount(account.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error refreshing account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
