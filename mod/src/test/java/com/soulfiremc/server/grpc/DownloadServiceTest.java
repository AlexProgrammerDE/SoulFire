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

import com.soulfiremc.grpc.generated.DownloadRequest;
import com.soulfiremc.grpc.generated.DownloadResponse;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Context;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

final class DownloadServiceTest {
  private final DownloadServiceImpl service = new DownloadServiceImpl();
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));

  @ParameterizedTest
  @CsvSource({"200,''", "204,''", "200,content", "404,missing"})
  void downloadsCompleteWithStatusAndHeadersEvenWithoutContent(int status, String body) throws Exception {
    var server = HttpServer.create().host("127.0.0.1").port(0)
      .handle((_, response) -> response.status(status).header("X-Download-Test", "present").sendString(Mono.just(body)))
      .bindNow();
    try {
      var result = new CompletableFuture<DownloadResponse>();
      context.run(() -> service.download(request(server.port()), new StreamObserver<>() {
        private DownloadResponse response;

        @Override
        public void onNext(DownloadResponse value) {
          response = value;
        }

        @Override
        public void onError(Throwable error) {
          result.completeExceptionally(error);
        }

        @Override
        public void onCompleted() {
          result.complete(response);
        }
      }));

      var response = result.get(5, TimeUnit.SECONDS);
      assertNotNull(response);
      assertEquals(status, response.getStatusCode());
      assertEquals(body, response.getData().toStringUtf8());
      assertTrue(response.getHeadersList().stream().anyMatch(header ->
        header.getKey().equalsIgnoreCase("X-Download-Test") && header.getValue().equals("present")));
    } finally {
      server.disposeNow();
    }
  }

  @Test
  void cancellingAnRpcClosesItsPendingHttpRequest() throws Exception {
    var started = new CountDownLatch(1);
    var closed = new CountDownLatch(1);
    var server = HttpServer.create().host("127.0.0.1").port(0)
      .handle((request, response) -> {
        request.withConnection(connection -> connection.onDispose(closed::countDown));
        started.countDown();
        return response.sendString(Mono.never());
      }).bindNow();
    try {
      @SuppressWarnings("unchecked")
      var observer = (ServerCallStreamObserver<DownloadResponse>) mock(ServerCallStreamObserver.class);
      var cancel = new AtomicReference<Runnable>();
      doAnswer(invocation -> {
        cancel.set(invocation.getArgument(0));
        return null;
      }).when(observer).setOnCancelHandler(any());

      context.run(() -> service.download(request(server.port()), observer));
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertNotNull(cancel.get());
      cancel.get().run();
      assertTrue(closed.await(2, TimeUnit.SECONDS));
      verify(observer, never()).onNext(any());
      verify(observer, never()).onCompleted();
    } finally {
      server.disposeNow();
    }
  }

  private static DownloadRequest request(int port) {
    return DownloadRequest.newBuilder().setInstanceId(UUID.randomUUID().toString())
      .setUri("http://127.0.0.1:" + port + "/download").build();
  }
}
