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
import com.soulfiremc.server.protocol.BotConnectionFactory;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.SFNettyHelper;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CancellationCollector;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;

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
      var isBedrock = SFVersionConstants.isBedrock(protocolVersion);
      var targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsSource, ProxySettings.PROXY_CHECK_ADDRESS)
        .orElseThrow(() -> new IllegalStateException("Could not resolve address"));
      var proxyCheckEventLoopGroup =
        SFNettyHelper.createEventLoopGroup("ProxyCheck-%s".formatted(UUID.randomUUID().toString()), instance.runnableWrapper());
      instance.scheduler().runAsync(() -> {
        var results = SFHelpers.maxFutures(settingsSource.get(ProxySettings.PROXY_CHECK_CONCURRENCY), request.getProxyList(), payload -> {
            var stopWatch = Stopwatch.createStarted();
            var factory = new BotConnectionFactory(
              instance,
              targetAddress,
              settingsSource,
              MinecraftAccount.forProxyCheck(),
              protocolVersion,
              SFProxy.fromProto(payload),
              proxyCheckEventLoopGroup
            );
            return instance.scheduler().supplyAsync(() -> {
                var future = cancellationCollector.add(new CompletableFuture<Void>());
                var connection = factory.prepareConnectionInternal(ProtocolState.STATUS);

                connection.session().addListener(new SessionAdapter() {
                  @Override
                  public void packetReceived(Session session, Packet packet) {
                    if (future.isDone()) {
                      return;
                    }

                    if (packet instanceof ClientboundStatusResponsePacket) {
                      future.complete(null);
                    }
                  }

                  @Override
                  public void packetError(PacketErrorEvent event) {
                    if (future.isDone()) {
                      return;
                    }

                    future.completeExceptionally(event.getCause());
                  }

                  @Override
                  public void disconnected(DisconnectedEvent event) {
                    if (future.isDone()) {
                      return;
                    }

                    future.completeExceptionally(event.getCause());
                  }
                });

                connection.connect().join();

                return future.join();
              })
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

            if (result.getValid()) {
              responseObserver.onNext(ProxyCheckResponse.newBuilder()
                .setOneSuccess(ProxyCheckOneSuccess.newBuilder()
                  .build())
                .build());
            } else {
              responseObserver.onNext(ProxyCheckResponse.newBuilder()
                .setOneFailure(ProxyCheckOneFailure.newBuilder()
                  .build())
                .build());
            }
          },
          cancellationCollector);

        proxyCheckEventLoopGroup.shutdownGracefully()
          .awaitUninterruptibly(5, TimeUnit.SECONDS);

        if (responseObserver.isCancelled()) {
          return;
        }

        responseObserver.onNext(ProxyCheckResponse.newBuilder()
          .setFullList(ProxyCheckFullList.newBuilder()
            .addAllResponse(results)
            .build())
          .build());
        responseObserver.onCompleted();
      });
    } catch (Throwable t) {
      log.error("Error checking proxy", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
