/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.grpc;

import io.grpc.*;
import io.jsonwebtoken.*;

import java.security.Key;

public class JwtServerInterceptor implements ServerInterceptor {
    private final JwtParser parser;

    public JwtServerInterceptor(Key jwtKey) {
        parser = Jwts.parserBuilder().setSigningKey(jwtKey).build();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        var value = metadata.get(Constant.AUTHORIZATION_METADATA_KEY);

        var status = Status.OK;
        if (value == null) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(Constant.BEARER_TYPE)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            Jws<Claims> claims = null;
            // remove authorization type prefix
            var token = value.substring(Constant.BEARER_TYPE.length()).trim();
            try {
                // verify token signature and parse claims
                claims = parser.parseClaimsJws(token);
            } catch (JwtException e) {
                status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
            if (claims != null) {
                // set client id into current context
                var ctx = Context.current()
                        .withValue(Constant.CLIENT_ID_CONTEXT_KEY, claims.getBody().getSubject());
                return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
            }
        }

        serverCall.close(status, new Metadata());
        return new ServerCall.Listener<>() {
            // noop
        };
    }
}
