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
import com.soulfiremc.server.account.*;
import com.soulfiremc.server.user.Permissions;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MCAuthServiceImpl extends MCAuthServiceGrpc.MCAuthServiceImplBase {
  private static @Nullable SFProxy convertProxy(BooleanSupplier hasProxy, Supplier<ProxyProto> proxy) {
    return hasProxy.getAsBoolean() ? SFProxy.fromProto(proxy.get()) : null;
  }

  @Override
  public void login(AuthRequest request, StreamObserver<AuthResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    try {
      var account = MCAuthService.convertService(request.getService()).createDataAndLogin(request.getPayload(),
        convertProxy(request::hasProxy, request::getProxy));

      responseObserver.onNext(AuthResponse.newBuilder().setAccount(account.join().toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error authenticating account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void refresh(RefreshRequest request, StreamObserver<RefreshResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    try {
      var receivedAccount = MinecraftAccount.fromProto(request.getAccount());
      var account = MCAuthService.convertService(request.getAccount().getType()).refresh(receivedAccount,
        convertProxy(request::hasProxy, request::getProxy)).join();

      responseObserver.onNext(RefreshResponse.newBuilder().setAccount(account.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error refreshing account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
