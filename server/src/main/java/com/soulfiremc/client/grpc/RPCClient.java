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

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.grpc.generated.AttackServiceGrpc;
import com.soulfiremc.grpc.generated.CommandServiceGrpc;
import com.soulfiremc.grpc.generated.ConfigServiceGrpc;
import com.soulfiremc.grpc.generated.LogsServiceGrpc;
import com.soulfiremc.grpc.generated.MCAuthServiceGrpc;
import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RPCClient {
  private final List<Context.CancellableContext> contexts = new ArrayList<>();
  private final ManagedChannel channel;
  private final LogsServiceGrpc.LogsServiceBlockingStub logStubBlocking;
  private final CommandServiceGrpc.CommandServiceStub commandStub;
  private final CommandServiceGrpc.CommandServiceBlockingStub commandStubBlocking;
  private final AttackServiceGrpc.AttackServiceStub attackStub;
  private final ConfigServiceGrpc.ConfigServiceBlockingStub configStubBlocking;
  private final MCAuthServiceGrpc.MCAuthServiceBlockingStub mcAuthServiceBlocking;

  public RPCClient(String host, int port, String jwt) {
    this(
        new JwtCredential(jwt),
        Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
            .userAgent("SoulFireJavaClient/" + BuildData.VERSION)
            .build());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("*** shutting down gRPC client since JVM is shutting down");
                  try {
                    shutdown();
                  } catch (Throwable e) {
                    log.error("Interrupted while shutting down gRPC client", e);
                    return;
                  }
                  log.info("*** client shut down");
                }));
  }

  public RPCClient(CallCredentials callCredentials, ManagedChannel managedChannel) {
    channel = managedChannel;
    logStubBlocking = prepareChannel(LogsServiceGrpc.newBlockingStub(channel), callCredentials);
    commandStub = prepareChannel(CommandServiceGrpc.newStub(channel), callCredentials);
    commandStubBlocking =
        prepareChannel(CommandServiceGrpc.newBlockingStub(channel), callCredentials);
    attackStub = prepareChannel(AttackServiceGrpc.newStub(channel), callCredentials);
    configStubBlocking =
        prepareChannel(ConfigServiceGrpc.newBlockingStub(channel), callCredentials);
    mcAuthServiceBlocking = prepareChannel(MCAuthServiceGrpc.newBlockingStub(channel), callCredentials);
  }

  private <T extends AbstractStub<T>> T prepareChannel(T channel, CallCredentials callCredentials) {
    return channel
        .withCallCredentials(callCredentials)
        .withCompression("gzip")
        .withMaxInboundMessageSize(Integer.MAX_VALUE)
        .withMaxOutboundMessageSize(Integer.MAX_VALUE);
  }

  public void shutdown() throws InterruptedException {
    for (var context : contexts) {
      context.cancel(new RuntimeException("Shutting down"));
    }

    if (!channel.shutdown().awaitTermination(3, TimeUnit.SECONDS)
        && !channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)) {
      throw new RuntimeException("Unable to shutdown gRPC client");
    }
  }
}
