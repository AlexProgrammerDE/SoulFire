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
import com.soulfiremc.grpc.generated.LoginServiceGrpc;
import com.soulfiremc.server.util.RPCConstants;
import io.grpc.*;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LoginRateLimitingInterceptor implements ServerInterceptor {
  private final Cache<UUID, Integer> callCache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> serverCall,
    Metadata metadata,
    ServerCallHandler<ReqT, RespT> serverCallHandler) {

    var status = Status.OK;
    if (Objects.equals(serverCall.getMethodDescriptor().getServiceName(), LoginServiceGrpc.SERVICE_NAME)) {
      var remoteAddr = metadata.get(Metadata.Key.of("origin", Metadata.ASCII_STRING_MARSHALLER));
      if (remoteAddr == null) {
        status = Status.UNAUTHENTICATED.withDescription("No remote address");
      } else {
        var key = UUID.nameUUIDFromBytes(remoteAddr.getBytes(StandardCharsets.UTF_8));
        var count = callCache.getIfPresent(key);
        if (count == null) {
          callCache.put(key, 1);
        } else if (count < RPCConstants.LOGIN_RATE_LIMIT) {
          callCache.put(key, count + 1);
        }

        if (count != null && count >= RPCConstants.LOGIN_RATE_LIMIT) {
          status = Status.RESOURCE_EXHAUSTED.withDescription("Too many login attempts");
        } else {
          return Contexts.interceptCall(
            Context.current(),
            serverCall,
            metadata,
            serverCallHandler
          );
        }
      }

      serverCall.close(status, new Metadata());
      return new ServerCall.Listener<>() {
        // noop
      };
    } else {
      return Contexts.interceptCall(
        Context.current(),
        serverCall,
        metadata,
        serverCallHandler
      );
    }
  }
}
