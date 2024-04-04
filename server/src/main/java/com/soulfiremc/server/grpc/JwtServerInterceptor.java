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

import com.soulfiremc.util.RPCConstants;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;

public class JwtServerInterceptor implements ServerInterceptor {
  private final JwtParser parser;
  private final AuthSystem authSystem;

  public JwtServerInterceptor(SecretKey jwtKey, AuthSystem authSystem) {
    this.parser = Jwts.parser().verifyWith(jwtKey).build();
    this.authSystem = authSystem;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> serverCall,
    Metadata metadata,
    ServerCallHandler<ReqT, RespT> serverCallHandler) {
    var value = metadata.get(RPCConstants.AUTHORIZATION_METADATA_KEY);

    var status = Status.OK;
    if (value == null) {
      status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
    } else if (!value.startsWith(RPCConstants.BEARER_TYPE)) {
      status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
    } else {
      Jws<Claims> claims = null;
      // remove authorization type prefix
      var token = value.substring(RPCConstants.BEARER_TYPE.length()).trim();
      try {
        // verify token signature and parse claims
        claims = parser.parseSignedClaims(token);
      } catch (JwtException e) {
        status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
      }
      if (claims != null) {
        // set client id into current context
        var ctx =
          Context.current()
            .withValue(
              ServerRPCConstants.CLIENT_ID_CONTEXT_KEY, claims.getPayload().getSubject())
            .withValue(
              ServerRPCConstants.USER_CONTEXT_KEY,
              authSystem.authenticate(
                claims.getPayload().getSubject(), claims.getPayload().getIssuedAt()));
        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
      }
    }

    serverCall.close(status, new Metadata());
    return new ServerCall.Listener<>() {
      // noop
    };
  }
}
