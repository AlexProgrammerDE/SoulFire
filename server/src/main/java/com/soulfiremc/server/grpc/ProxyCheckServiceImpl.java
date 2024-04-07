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
import com.soulfiremc.grpc.generated.ProxyCheckRequest;
import com.soulfiremc.grpc.generated.ProxyCheckResponse;
import com.soulfiremc.grpc.generated.ProxyCheckResponseSingle;
import com.soulfiremc.grpc.generated.ProxyCheckServiceGrpc;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.ReactorHttpHelper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.netty.handler.codec.http.HttpStatusClass;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProxyCheckServiceImpl extends ProxyCheckServiceGrpc.ProxyCheckServiceImplBase {
  @Override
  public void check(
    ProxyCheckRequest request, StreamObserver<ProxyCheckResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.CHECK_PROXY);

    try {
      var url =
        switch (request.getTarget()) {
          case IPIFY -> ReactorHttpHelper.createURL("https://api.ipify.org");
          case AWS -> ReactorHttpHelper.createURL("https://checkip.amazonaws.com");
          case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized target");
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
