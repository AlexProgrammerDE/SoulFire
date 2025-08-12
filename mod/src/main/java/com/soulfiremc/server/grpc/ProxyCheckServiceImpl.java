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

import com.google.common.base.Stopwatch;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotPacketPreReceiveEvent;
import com.soulfiremc.server.bot.BotConnectionFactory;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.netty.NettyHelper;
import com.soulfiremc.server.util.structs.CancellationCollector;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import org.slf4j.event.Level;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class ProxyCheckServiceImpl extends ProxyCheckServiceGrpc.ProxyCheckServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void check(
    ProxyCheckRequest request, StreamObserver<ProxyCheckResponse> casted) {
    var responseObserver = (ServerCallStreamObserver<ProxyCheckResponse>) casted;
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CHECK_PROXY, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settingsSource = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      var protocolVersion = settingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
      var serverAddress = ServerAddress.parseString(settingsSource.get(ProxySettings.PROXY_CHECK_ADDRESS));
      var proxyCheckEventLoopGroup =
        NettyHelper.createEventLoopGroup("ProxyCheck-%s".formatted(UUID.randomUUID().toString()), instance.runnableWrapper());
      instance.scheduler().execute(() -> {
        SFHelpers.maxFutures(settingsSource.get(ProxySettings.PROXY_CHECK_CONCURRENCY), request.getProxyList(), payload -> {
            var proxy = SFProxy.fromProto(payload);
            var stopWatch = Stopwatch.createStarted();
            var factory = new BotConnectionFactory(
              instance,
              settingsSource,
              MinecraftAccount.forProxyCheck(),
              protocolVersion,
              serverAddress,
              proxy,
              proxyCheckEventLoopGroup
            );
            return instance.scheduler().supplyAsync(() -> {
                var future = cancellationCollector.add(new CompletableFuture<Void>());
                var connection = factory.prepareConnection(true);

                connection.shutdownHooks().add(() -> future.completeExceptionally(new RuntimeException("Connection closed")));

                SoulFireAPI.registerListener(BotPacketPreReceiveEvent.class, event -> {
                  if (event.connection() == connection && event.packet() instanceof ClientboundStatusResponsePacket) {
                    future.complete(null);
                  }
                });

                log.debug("Checking proxy: {}", proxy);
                connection.connect().join();

                return future.join();
              }, Level.TRACE)
              .orTimeout(30, TimeUnit.SECONDS)
              .handle((result, throwable) -> ProxyCheckResponseSingle.newBuilder()
                .setProxy(payload)
                .setLatency((int) stopWatch.stop().elapsed(TimeUnit.MILLISECONDS))
                .setValid(throwable == null)
                .build());
          }, result -> {
            if (responseObserver.isCancelled()) {
              return;
            }

            var proxy = SFProxy.fromProto(result.getProxy());
            if (result.getValid()) {
              log.debug("Proxy check successful for {}: {}ms", proxy, result.getLatency());
            } else {
              log.debug("Proxy check failed for {}", proxy);
            }

            responseObserver.onNext(ProxyCheckResponse.newBuilder()
              .setSingle(result)
              .build());
          },
          cancellationCollector);

        proxyCheckEventLoopGroup.shutdownGracefully()
          .awaitUninterruptibly(5, TimeUnit.SECONDS);

        if (responseObserver.isCancelled()) {
          return;
        }

        responseObserver.onNext(ProxyCheckResponse.newBuilder()
          .setEnd(ProxyCheckEnd.getDefaultInstance())
          .build());
        responseObserver.onCompleted();
      });
    } catch (Throwable t) {
      log.error("Error checking proxy", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
