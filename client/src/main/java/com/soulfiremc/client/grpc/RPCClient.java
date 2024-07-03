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
package com.soulfiremc.client.grpc;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import com.linecorp.armeria.client.grpc.GrpcClients;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.grpc.generated.AttackServiceGrpc;
import com.soulfiremc.grpc.generated.CommandServiceGrpc;
import com.soulfiremc.grpc.generated.ConfigServiceGrpc;
import com.soulfiremc.grpc.generated.LogsServiceGrpc;
import com.soulfiremc.grpc.generated.MCAuthServiceGrpc;
import com.soulfiremc.grpc.generated.ProxyCheckServiceGrpc;
import io.grpc.Codec;
import io.grpc.Context;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RPCClient {
  private final List<Context.CancellableContext> contexts = new ArrayList<>();
  private final LogsServiceGrpc.LogsServiceBlockingStub logStubBlocking;
  private final CommandServiceGrpc.CommandServiceStub commandStub;
  private final CommandServiceGrpc.CommandServiceBlockingStub commandStubBlocking;
  private final AttackServiceGrpc.AttackServiceStub attackStub;
  private final ConfigServiceGrpc.ConfigServiceBlockingStub configStubBlocking;
  private final MCAuthServiceGrpc.MCAuthServiceBlockingStub mcAuthServiceBlocking;
  private final ProxyCheckServiceGrpc.ProxyCheckServiceBlockingStub proxyCheckServiceBlocking;

  @SuppressWarnings("HttpUrlsUsage")
  public RPCClient(String host, int port, String jwt) {
    this(
      GrpcClients.builder("http://%s:%d".formatted(host, port))
        .serializationFormat(GrpcSerializationFormats.PROTO)
        .compressor(new Codec.Gzip())
        .callCredentials(new JwtCredential(jwt))
        .maxRequestMessageLength(Integer.MAX_VALUE)
        .maxResponseMessageLength(Integer.MAX_VALUE)
        // Allow long-lasting streams from the server
        .responseTimeout(Duration.ZERO)
        .setHeader(HttpHeaderNames.USER_AGENT, "SoulFireJavaClient/" + BuildData.VERSION));
  }

  public RPCClient(GrpcClientBuilder clientBuilder) {
    var factory = ClientFactory.builder().tlsNoVerify().build();
    factory.closeOnJvmShutdown();

    clientBuilder.factory(factory);

    logStubBlocking = clientBuilder.build(LogsServiceGrpc.LogsServiceBlockingStub.class);
    commandStub = clientBuilder.build(CommandServiceGrpc.CommandServiceStub.class);
    commandStubBlocking = clientBuilder.build(CommandServiceGrpc.CommandServiceBlockingStub.class);
    attackStub = clientBuilder.build(AttackServiceGrpc.AttackServiceStub.class);
    configStubBlocking = clientBuilder.build(ConfigServiceGrpc.ConfigServiceBlockingStub.class);
    mcAuthServiceBlocking = clientBuilder.build(MCAuthServiceGrpc.MCAuthServiceBlockingStub.class);
    proxyCheckServiceBlocking =
      clientBuilder.build(ProxyCheckServiceGrpc.ProxyCheckServiceBlockingStub.class);
  }
}
