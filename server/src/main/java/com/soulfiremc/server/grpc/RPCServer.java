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
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.grpc.GrpcMeterIdPrefixFunction;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.common.grpc.protocol.GrpcHeaderNames;
import com.linecorp.armeria.common.logging.LogFormatter;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.common.logging.LogWriter;
import com.linecorp.armeria.common.prometheus.PrometheusMeterRegistries;
import com.linecorp.armeria.server.RedirectService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import com.linecorp.armeria.server.prometheus.PrometheusExpositionService;
import com.soulfiremc.server.user.AuthSystem;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The RPC server for the SoulFire server.
 * This server is used to communicate with the SoulFire client.
 * The CLI may also use this RPC server to communicate with the SoulFire server.
 */
@Slf4j
public class RPCServer {
  @Getter
  private final String host;
  @Getter
  private final int port;
  private final Server server;
  private final Server prometheusServer;

  public RPCServer(
    String host,
    int port,
    Injector injector,
    AuthSystem authSystem) {
    this.host = host;
    this.port = port;

    var meterRegistry = PrometheusMeterRegistries.defaultRegistry();
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
        .intercept(List.of(
          new LoginRateLimitingInterceptor(),
          new JwtServerInterceptor(authSystem)
        ))
        .addService(injector.getSingleton(LogServiceImpl.class))
        .addService(injector.getSingleton(ConfigServiceImpl.class))
        .addService(injector.getSingleton(CommandServiceImpl.class))
        .addService(injector.getSingleton(InstanceServiceImpl.class))
        .addService(injector.getSingleton(MCAuthServiceImpl.class))
        .addService(injector.getSingleton(ProxyCheckServiceImpl.class))
        .addService(injector.getSingleton(DownloadServiceImpl.class))
        .addService(injector.getSingleton(ObjectStorageServiceImpl.class))
        .addService(injector.getSingleton(ServerServiceImpl.class))
        .addService(injector.getSingleton(UserServiceImpl.class))
        .addService(injector.getSingleton(LoginServiceImpl.class))
        // Allow collecting info about callable methods.
        .addService(ProtoReflectionServiceV1.newInstance())
        .maxRequestMessageLength(Integer.MAX_VALUE)
        .maxResponseMessageLength(Integer.MAX_VALUE)
        .supportedSerializationFormats(GrpcSerializationFormats.values())
        .enableUnframedRequests(true)
        .enableHealthCheckService(true)
        .build();

    var serverBuilder = Server.builder();
    var serverProtocols = new ArrayList<SessionProtocol>();
    if (Boolean.getBoolean("sf.grpc.proxy-protocol")) {
      serverProtocols.add(SessionProtocol.PROXY);
    }

    serverProtocols.add(SessionProtocol.HTTP);

    new AcmeClient().provisionAcmeCertIfNeeded();
    if (Boolean.getBoolean("sf.grpc.tls.enabled")) {
      serverProtocols.add(SessionProtocol.HTTPS);

      serverBuilder.tls(TlsKeyPair.of(
        Path.of(System.getProperty("sf.grpc.tls.key")).toFile(),
        System.getProperty("sf.grpc.tls.key.password"),
        Path.of(System.getProperty("sf.grpc.tls.cert")).toFile()
      ));
    }

    server =
      serverBuilder
        .port(new InetSocketAddress(host, port), serverProtocols)
        .meterRegistry(meterRegistry)
        .decorator(LoggingService.builder()
          .logWriter(LogWriter.builder()
            .logger(log)
            .responseLogLevelMapper(l -> {
              if (l.responseCause() instanceof StatusRuntimeException e && e.getStatus().getCode() == Status.Code.CANCELLED) {
                return LogLevel.DEBUG;
              }

              return null;
            })
            .logFormatter(LogFormatter.builderForText()
              .contentSanitizer((requestContext, o) -> "****")
              .build())
            .build())
          .newDecorator())
        .service(grpcService,
          corsBuilder.newDecorator(),
          MetricCollectingService.newDecorator(GrpcMeterIdPrefixFunction.of("soulfire")))
        .service("/health", HealthCheckService.builder().build())
        .service("/", new RedirectService("/docs"))
        .serviceUnder("/docs", DocService.builder()
          .exampleHeaders(HttpHeaders.of(HttpHeaderNames.AUTHORIZATION, "bearer <jwt>"))
          .build())
        .build();
    if (Boolean.getBoolean("sf.prometheus.enabled")) {
      prometheusServer =
        Server.builder()
          .port(new InetSocketAddress(host, Integer.getInteger("sf.prometheus.port", 9090)), SessionProtocol.HTTP)
          .service("/metrics", PrometheusExpositionService.of(meterRegistry.getPrometheusRegistry()))
          .build();
    } else {
      prometheusServer = null;
    }
  }

  public void start() throws IOException {
    server.closeOnJvmShutdown();

    server.start().join();

    log.info("RPC Server started, listening on {}", server.activeLocalPort());

    if (prometheusServer != null) {
      prometheusServer.closeOnJvmShutdown();
      prometheusServer.start().join();
      log.info("Prometheus Server started, listening on {}", prometheusServer.activeLocalPort());
    }
  }

  public void shutdown() throws InterruptedException {
    server.stop().join();

    if (prometheusServer != null) {
      prometheusServer.stop().join();
    }
  }
}
