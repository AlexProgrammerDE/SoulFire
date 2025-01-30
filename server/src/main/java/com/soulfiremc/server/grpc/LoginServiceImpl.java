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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soulfiremc.grpc.generated.EmailCodeRequest;
import com.soulfiremc.grpc.generated.LoginRequest;
import com.soulfiremc.grpc.generated.LoginServiceGrpc;
import com.soulfiremc.grpc.generated.NextAuthFlowResponse;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.UserEntity;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginServiceImpl extends LoginServiceGrpc.LoginServiceImplBase {
  private final Cache<UUID, FlowStage> authFlows = Caffeine.newBuilder()
    .expireAfterWrite(15, TimeUnit.MINUTES)
    .build();
  private final SoulFireServer soulFireServer;
  private final SessionFactory sessionFactory;

  private static String generateSixDigitCode() {
    var random = ThreadLocalRandom.current();
    return String.valueOf(random.nextInt(100000, 999999));
  }

  @Override
  public void login(LoginRequest request, StreamObserver<NextAuthFlowResponse> responseObserver) {
    try {
      var authFlowToken = UUID.randomUUID();
      var user = sessionFactory.fromTransaction(session -> session.createQuery("from UserEntity where email = :email", UserEntity.class)
        .setParameter("email", request.getEmail())
        .uniqueResult());

      // To prevent people checking if an email is registered,
      // we always return a flow token, even if the email is not registered
      if (user != null) {
        var emailCode = generateSixDigitCode();
        authFlows.put(authFlowToken, new EmailFlowStage(user.id(), emailCode));
        soulFireServer.emailSender().sendLoginCode(user.email(), user.username(), emailCode);
      }

      responseObserver.onNext(NextAuthFlowResponse.newBuilder()
        .setAuthFlowToken(authFlowToken.toString())
        .setEmailCode(NextAuthFlowResponse.EmailCode.newBuilder().build())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error logging in", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void emailCode(EmailCodeRequest request, StreamObserver<NextAuthFlowResponse> responseObserver) {
    try {
      var authFlowToken = UUID.fromString(request.getAuthFlowToken());
      var flowStage = authFlows.getIfPresent(authFlowToken);
      // Not present also returns invalid code
      // This way we prevent bruteforce attacks
      if (!(flowStage instanceof EmailFlowStage(var userId, var code))
        || !code.equals(request.getCode())) {
        responseObserver.onNext(NextAuthFlowResponse.newBuilder()
          .setAuthFlowToken(authFlowToken.toString())
          .setFailure(NextAuthFlowResponse.Failure.newBuilder()
            .setReason(NextAuthFlowResponse.Failure.Reason.INVALID_CODE)
            .build())
          .build());
        responseObserver.onCompleted();
      } else {
        authFlows.invalidate(authFlowToken);
        responseObserver.onNext(NextAuthFlowResponse.newBuilder()
          .setAuthFlowToken(authFlowToken.toString())
          .setSuccess(NextAuthFlowResponse.Success.newBuilder()
            .setToken(soulFireServer.authSystem().generateJWT(
              soulFireServer.authSystem().getUserData(userId).orElseThrow()))
            .build())
          .build());
        responseObserver.onCompleted();
      }
    } catch (Throwable t) {
      log.error("Error verifying email code", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private interface FlowStage {
  }

  private record EmailFlowStage(UUID userId, String code) implements FlowStage {
  }
}
