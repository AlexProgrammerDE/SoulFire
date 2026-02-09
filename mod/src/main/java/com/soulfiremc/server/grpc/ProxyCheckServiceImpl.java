/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import com.google.common.base.Stopwatch;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.OfflineAuthService;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotPacketPreReceiveEvent;
import com.soulfiremc.server.bot.BotConnectionFactory;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.settings.lib.BotSettingsImpl;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CancellationCollector;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public final class ProxyCheckServiceImpl extends ProxyCheckServiceGrpc.ProxyCheckServiceImplBase {
  private static final MinecraftAccount PROXY_CHECK_ACCOUNT = OfflineAuthService.createAccount("ProxyCheck");
  private final SoulFireServer soulFireServer;

  @Override
  public void check(
    ProxyCheckRequest request, StreamObserver<ProxyCheckResponse> casted) {
    var responseObserver = (ServerCallStreamObserver<ProxyCheckResponse>) casted;
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CHECK_PROXY, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)).asRuntimeException();
    }

    var instance = optionalInstance.get();
    var settingsSource = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      instance.scheduler().execute(() -> {
        try {
          SFHelpers.maxFutures(instance.scheduler(), settingsSource.get(ProxySettings.PROXY_CHECK_CONCURRENCY), request.getProxyList(), payload -> {
              var proxy = SFProxy.fromProto(payload);
              var stopWatch = Stopwatch.createStarted();
              var botSettingsSource = new BotSettingsImpl(PROXY_CHECK_ACCOUNT, settingsSource);
              var protocolVersion = botSettingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
              var serverAddress = BotConnectionFactory.parseAddress(settingsSource.get(ProxySettings.PROXY_CHECK_ADDRESS), protocolVersion);
              var factory = new BotConnectionFactory(
                instance,
                botSettingsSource,
                protocolVersion,
                serverAddress,
                proxy
              );
              var valid = false;
              var connection = factory.prepareConnection(true);
              try {
                var latch = new CountDownLatch(1);

                connection.shutdownHooks().add(latch::countDown);

                Consumer<BotPacketPreReceiveEvent> listener = event -> {
                  if (event.connection() == connection && event.packet() instanceof ClientboundStatusResponsePacket) {
                    latch.countDown();
                  }
                };
                SoulFireAPI.registerListener(BotPacketPreReceiveEvent.class, listener);

                try {
                  log.debug("Checking proxy: {}", proxy);
                  connection.connect().join();

                  valid = latch.await(30, TimeUnit.SECONDS);
                } finally {
                  SoulFireAPI.unregisterListener(BotPacketPreReceiveEvent.class, listener);
                }
              } catch (Throwable t) {
                log.trace("Proxy check error for {}", proxy, t);
              } finally {
                connection.disconnect(Component.text("Proxy check completed"));
              }

              var result = ProxyCheckResponseSingle.newBuilder()
                .setProxy(payload)
                .setLatency((int) stopWatch.stop().elapsed(TimeUnit.MILLISECONDS))
                .setValid(valid)
                .build();

              synchronized (responseObserver) {
                if (responseObserver.isCancelled()) {
                  return;
                }

                if (result.getValid()) {
                  log.debug("Proxy check successful for {}: {}ms", proxy, result.getLatency());
                } else {
                  log.debug("Proxy check failed for {}", proxy);
                }

                responseObserver.onNext(ProxyCheckResponse.newBuilder()
                  .setSingle(result)
                  .build());
              }
            },
            cancellationCollector);

          synchronized (responseObserver) {
            if (responseObserver.isCancelled()) {
              return;
            }

            responseObserver.onNext(ProxyCheckResponse.newBuilder()
              .setEnd(ProxyCheckEnd.getDefaultInstance())
              .build());
            responseObserver.onCompleted();
          }
        } catch (Throwable t) {
          log.error("Error during async proxy check", t);
          synchronized (responseObserver) {
            if (!responseObserver.isCancelled()) {
              responseObserver.onError(Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException());
            }
          }
        }
      });
    } catch (Throwable t) {
      log.error("Error checking proxy", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }
}
