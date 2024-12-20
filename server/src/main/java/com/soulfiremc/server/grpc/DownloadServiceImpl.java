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

import com.google.protobuf.ByteString;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.ReactorHttpHelper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public class DownloadServiceImpl extends DownloadServiceGrpc.DownloadServiceImplBase {
  public static @Nullable SFProxy convertProxy(BooleanSupplier hasProxy, Supplier<ProxyProto> proxy) {
    return hasProxy.getAsBoolean() ? SFProxy.fromProto(proxy.get()) : null;
  }

  @Override
  public void download(DownloadRequest request, StreamObserver<DownloadResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DOWNLOAD_URL, instanceId));

    try {
      var proxy = convertProxy(request::hasProxy, request::getProxy);
      ReactorHttpHelper.createReactorClient(proxy, false)
        .headers(h -> request.getHeadersList().forEach(header -> h.set(header.getKey(), header.getValue())))
        .get()
        .uri(URI.create(request.getUri()))
        .responseSingle(
          (res, content) ->
            content
              .asByteArray()
              .map(
                responseBytes ->
                  DownloadResponse.newBuilder()
                    .setData(ByteString.copyFrom(responseBytes))
                    .addAllHeaders(res.responseHeaders().entries().stream().map(h -> HeaderPair.newBuilder()
                      .setKey(h.getKey())
                      .setValue(h.getValue())
                      .build()).toList())
                    .setStatusCode(res.status().code())
                    .build()))
        .subscribe(response -> {
          responseObserver.onNext(response);
          responseObserver.onCompleted();
        }, t -> {
          log.error("Error downloading data", t);
          responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t)));
        });
    } catch (Throwable t) {
      log.error("Error downloading data", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
