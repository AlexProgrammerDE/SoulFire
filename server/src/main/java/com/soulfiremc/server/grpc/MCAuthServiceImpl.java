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
import com.soulfiremc.server.account.SFBedrockMicrosoftAuthService;
import com.soulfiremc.server.account.SFEasyMCAuthService;
import com.soulfiremc.server.account.SFJavaMicrosoftAuthService;
import com.soulfiremc.server.account.SFOfflineAuthService;
import com.soulfiremc.server.account.SFTheAlteningAuthService;
import com.soulfiremc.settings.proxy.SFProxy;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MCAuthServiceImpl extends MCAuthServiceGrpc.MCAuthServiceImplBase {
  private static @Nullable SFProxy convertProxy(AuthRequest request) {
    if (!request.hasProxy()) {
      return null;
    }

    return SFProxy.fromProto(request.getProxy());
  }

  @Override
  public void login(AuthRequest request, StreamObserver<AuthResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.AUTHENTICATE_MC_ACCOUNT);

    try {
      var account =
        (switch (request.getService()) {
          case MICROSOFT_JAVA -> new SFJavaMicrosoftAuthService();
          case MICROSOFT_BEDROCK -> new SFBedrockMicrosoftAuthService();
          case THE_ALTENING -> new SFTheAlteningAuthService();
          case EASY_MC -> new SFEasyMCAuthService();
          case OFFLINE -> new SFOfflineAuthService();
          case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
        })
          .createDataAndLogin(request.getPayload(), convertProxy(request));

      responseObserver.onNext(AuthResponse.newBuilder().setAccount(account.toProto()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error authenticating account", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
