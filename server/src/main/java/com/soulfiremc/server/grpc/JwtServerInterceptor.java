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

import com.soulfiremc.grpc.generated.LoginServiceGrpc;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.util.RPCConstants;
import io.grpc.*;
import io.jsonwebtoken.*;

import java.util.Objects;

public class JwtServerInterceptor implements ServerInterceptor {
  private final JwtParser parser;
  private final AuthSystem authSystem;

  public JwtServerInterceptor(AuthSystem authSystem) {
    this.parser = Jwts.parser().verifyWith(authSystem.jwtSecretKey()).build();
    this.authSystem = authSystem;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> serverCall,
    Metadata metadata,
    ServerCallHandler<ReqT, RespT> serverCallHandler) {

    // Login is not authed
    if (Objects.equals(serverCall.getMethodDescriptor().getServiceName(), LoginServiceGrpc.SERVICE_NAME)) {
      return Contexts.interceptCall(
        Context.current(),
        serverCall,
        metadata,
        serverCallHandler
      );
    }

    var status = Status.OK;
    var value = metadata.get(RPCConstants.AUTHORIZATION_METADATA_KEY);
    if (value == null) {
      status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
    } else if (!value.startsWith(RPCConstants.BEARER_TYPE)) {
      status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
    } else {
      Jws<Claims> claims = null;
      // remove authorization type prefix
      var token = value.substring(RPCConstants.BEARER_TYPE.length()).strip();
      try {
        // verify token signature and parse claims
        claims = parser.parseSignedClaims(token);
      } catch (JwtException e) {
        status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
      }
      if (claims != null) {
        var user = authSystem.authenticate(
          claims.getPayload().getSubject(), claims.getPayload().getIssuedAt().toInstant());

        if (user.isPresent()) {
          // set client id into current context
          return Contexts.interceptCall(
            Context.current()
              .withValue(
                ServerRPCConstants.USER_CONTEXT_KEY,
                user.get()),
            serverCall,
            metadata,
            serverCallHandler
          );
        } else {
          status = Status.UNAUTHENTICATED.withDescription("User not found");
        }
      }
    }

    serverCall.close(status, new Metadata());
    return new ServerCall.Listener<>() {
      // noop
    };
  }
}
