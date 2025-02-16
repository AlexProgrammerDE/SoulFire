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
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CancellationCollector;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.netty.handler.codec.http.HttpStatusClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProxyCheckServiceImpl extends ProxyCheckServiceGrpc.ProxyCheckServiceImplBase {
  private static final URL IPIFY_URL = ReactorHttpHelper.createURL("https://api.ipify.org");
  private static final URL AWS_URL = ReactorHttpHelper.createURL("https://checkip.amazonaws.com");
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
    var settings = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      var url = switch (settings.get(ProxySettings.PROXY_CHECK_SERVICE, ProxySettings.ProxyCheckService.class)) {
        case IPIFY -> IPIFY_URL;
        case AWS -> AWS_URL;
      };

      instance.scheduler().runAsync(() -> {
        var results = SFHelpers.maxFutures(settings.get(ProxySettings.PROXY_CHECK_CONCURRENCY), request.getProxyList(), payload -> {
            var client = ReactorHttpHelper.createReactorClient(SFProxy.fromProto(payload), false);
            var stopWatch = Stopwatch.createStarted();
            return cancellationCollector.add(client
              .get()
              .uri(url.toString())
              .responseSingle(
                (r, b) -> {
                  if (r.status().codeClass() == HttpStatusClass.SUCCESS) {
                    return b.asString();
                  }

                  return Mono.empty();
                })
              .timeout(Duration.ofSeconds(15))
              .onErrorResume(t -> Mono.empty())
              .map(response -> ProxyCheckResponseSingle.newBuilder()
                .setProxy(payload)
                .setLatency((int) stopWatch.stop().elapsed(TimeUnit.MILLISECONDS))
                .setValid(true)
                .setRealIp(response)
                .build())
              .switchIfEmpty(Mono.fromSupplier(() -> ProxyCheckResponseSingle.newBuilder()
                .setProxy(payload)
                .setLatency((int) stopWatch.stop().elapsed(TimeUnit.MILLISECONDS))
                .setValid(false)
                .build()))
              .toFuture());
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
