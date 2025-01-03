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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.netty.handler.codec.http.HttpStatusClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
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
    ProxyCheckRequest request, StreamObserver<ProxyCheckResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CHECK_PROXY, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settings = instance.settingsSource();

    try {
      var url = switch (settings.get(ProxySettings.PROXY_CHECK_SERVICE, ProxySettings.ProxyCheckService.class)) {
        case IPIFY -> IPIFY_URL;
        case AWS -> AWS_URL;
      };

      var responses = new ArrayList<ProxyCheckResponseSingle>();
      for (var proxy : request.getProxyList().stream().map(SFProxy::fromProto).toList()) {
        var client = ReactorHttpHelper.createReactorClient(proxy, false);
        var stopWatch = Stopwatch.createStarted();
        var response =
          client
            .get()
            .uri(url.toString())
            .responseSingle(
              (r, b) -> {
                if (r.status().codeClass() == HttpStatusClass.SUCCESS) {
                  return b.asString();
                }

                return Mono.empty();
              })
            .blockOptional();

        var single =
          ProxyCheckResponseSingle.newBuilder()
            .setProxy(proxy.toProto())
            .setLatency((int) stopWatch.stop().elapsed(TimeUnit.MILLISECONDS))
            .setValid(response.isPresent());

        response.ifPresent(single::setRealIp);

        responses.add(single.build());
      }

      responseObserver.onNext(ProxyCheckResponse.newBuilder().addAllResponse(responses).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error checking proxy", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
