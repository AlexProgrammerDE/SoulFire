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

import ch.jalu.injector.Injector;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.common.grpc.protocol.GrpcHeaderNames;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.grpc.GrpcService;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RPCServer {
  @Getter
  private final String host;
  @Getter
  private final int port;
  private final Server server;

  public RPCServer(
    String host, int port, Injector injector, SecretKey jwtKey, AuthSystem authSystem) {
    this(
      jwtKey,
      Server.builder()
        .tlsSelfSigned()
        .port(new InetSocketAddress(host, port), SessionProtocol.HTTP, SessionProtocol.HTTPS),
      host,
      port,
      injector,
      authSystem);
  }

  public RPCServer(
    SecretKey jwtKey,
    ServerBuilder serverBuilder,
    String host,
    int port,
    Injector injector,
    AuthSystem authSystem) {
    this.host = host;
    this.port = port;

    var corsBuilder =
      CorsService.builderForAnyOrigin()
        .allowRequestMethods(HttpMethod.POST) // Allow POST method.
        // Allow Content-type and X-GRPC-WEB headers.
        .allowRequestHeaders(
          HttpHeaderNames.CONTENT_TYPE,
          HttpHeaderNames.of("X-GRPC-WEB"),
          HttpHeaderNames.of("X-User-Agent"),
          HttpHeaderNames.AUTHORIZATION
        )
        // Expose trailers of the HTTP response to the client.
        .exposeHeaders(
          GrpcHeaderNames.GRPC_STATUS,
          GrpcHeaderNames.GRPC_MESSAGE,
          GrpcHeaderNames.ARMERIA_GRPC_THROWABLEPROTO_BIN);
    var grpcService =
      GrpcService.builder()
        .autoCompression(true)
        .intercept(new JwtServerInterceptor(jwtKey, authSystem))
        .addService(injector.getSingleton(LogServiceImpl.class))
        .addService(injector.getSingleton(ConfigServiceImpl.class))
        .addService(injector.getSingleton(CommandServiceImpl.class))
        .addService(injector.getSingleton(AttackServiceImpl.class))
        .addService(injector.getSingleton(MCAuthServiceImpl.class))
        .addService(injector.getSingleton(ProxyCheckServiceImpl.class))
        // Allow collecting info about callable methods.
        .addService(ProtoReflectionService.newInstance())
        .maxRequestMessageLength(Integer.MAX_VALUE)
        .maxResponseMessageLength(Integer.MAX_VALUE)
        .supportedSerializationFormats(GrpcSerializationFormats.values())
        .enableUnframedRequests(true)
        .enableHealthCheckService(true)
        .build();
    server =
      serverBuilder
        .service(grpcService, corsBuilder.newDecorator())
        .build();
  }

  public void start() throws IOException {
    server.closeOnJvmShutdown();

    server.start().join();

    log.info("RPC Server started, listening on {}", server.activeLocalPort());
  }

  public void shutdown() throws InterruptedException {
    server.stop().join();
  }
}
