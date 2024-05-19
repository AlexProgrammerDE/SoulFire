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

import com.soulfiremc.grpc.generated.AuthRequest;
import com.soulfiremc.grpc.generated.AuthResponse;
import com.soulfiremc.grpc.generated.MCAuthServiceGrpc;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.grpc.generated.ProxyProto;
import com.soulfiremc.grpc.generated.RefreshRequest;
import com.soulfiremc.grpc.generated.RefreshResponse;
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.SFBedrockMicrosoftAuthService;
import com.soulfiremc.server.account.SFEasyMCAuthService;
import com.soulfiremc.server.account.SFJavaMicrosoftAuthService;
import com.soulfiremc.server.account.SFOfflineAuthService;
import com.soulfiremc.server.account.SFTheAlteningAuthService;
import com.soulfiremc.server.user.Permissions;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MCAuthServiceImpl extends MCAuthServiceGrpc.MCAuthServiceImplBase {
  private static @Nullable SFProxy convertProxy(BooleanSupplier hasProxy, Supplier<ProxyProto> proxy) {
    return hasProxy.getAsBoolean() ? SFProxy.fromProto(proxy.get()) : null;
  }

  private static MCAuthService<?> convertService(MinecraftAccountProto.AccountTypeProto service) {
    return switch (service) {
      case MICROSOFT_JAVA -> new SFJavaMicrosoftAuthService();
      case MICROSOFT_BEDROCK -> new SFBedrockMicrosoftAuthService();
      case THE_ALTENING -> new SFTheAlteningAuthService();
      case EASY_MC -> new SFEasyMCAuthService();
      case OFFLINE -> new SFOfflineAuthService();
      case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
    };
  }

  @Override
  public void login(AuthRequest request, StreamObserver<AuthResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.AUTHENTICATE_MC_ACCOUNT);

    try {
      var account = convertService(request.getService()).createDataAndLogin(request.getPayload(),
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
      var account = convertService(request.getAccount().getType()).refresh(receivedAccount,
        convertProxy(request::hasProxy, request::getProxy)).join();

      responseObserver.onNext(RefreshResponse.newBuilder().setAccount(account.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error refreshing account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
