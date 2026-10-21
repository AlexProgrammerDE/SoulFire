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

import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RequestContextResponseInterceptorTest {
  @Test
  void dispatchesReentrantResponsesThroughTheirOriginalContext() throws Exception {
    var originalContext = context("/original");
    var reentrantContext = context("/reentrant");
    var delegate = new RecordingServerCall(originalContext, 3);
    var interceptedCall = new AtomicReference<ServerCall<String, String>>();

    try (var ignored = originalContext.push()) {
      new RequestContextResponseInterceptor().interceptCall(
        delegate,
        new Metadata(),
        (call, _) -> {
          interceptedCall.set(call);
          return new ServerCall.Listener<>() {};
        }
      );
    }

    try (var ignored = reentrantContext.push()) {
      interceptedCall.get().sendHeaders(new Metadata());
      interceptedCall.get().sendMessage("response");
      interceptedCall.get().close(Status.OK, new Metadata());
    }

    assertTrue(delegate.await());
    assertEquals(List.of("headers", "message:response", "close:OK"), delegate.operations());
    delegate.contexts().forEach(context -> assertSame(originalContext, context));
  }

  @Test
  void responsesInTheOriginalContextCannotOvertakeQueuedHeaders() throws Exception {
    var originalContext = context("/original");
    var delegate = new RecordingServerCall(originalContext, 3);
    var interceptedCall = new AtomicReference<ServerCall<String, String>>();

    try (var ignored = originalContext.push()) {
      new RequestContextResponseInterceptor().interceptCall(
        delegate,
        new Metadata(),
        (call, _) -> {
          interceptedCall.set(call);
          return new ServerCall.Listener<>() {};
        }
      );
    }
    var loopBlocked = new CountDownLatch(1);
    var releaseLoop = new CountDownLatch(1);
    originalContext.eventLoop().execute(() -> {
      loopBlocked.countDown();
      try {
        assertTrue(releaseLoop.await(5, TimeUnit.SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError(e);
      }
    });
    assertTrue(loopBlocked.await(5, TimeUnit.SECONDS));
    try {
      interceptedCall.get().sendHeaders(new Metadata());
      try (var ignored = originalContext.push()) {
        interceptedCall.get().sendMessage("response");
        interceptedCall.get().close(Status.OK, new Metadata());
      }
    } finally {
      releaseLoop.countDown();
    }
    assertTrue(delegate.await());
    assertEquals(List.of("headers", "message:response", "close:OK"), delegate.operations());
    delegate.contexts().forEach(context -> assertSame(originalContext, context));
  }

  private static ServiceRequestContext context(String path) {
    return ServiceRequestContext.of(HttpRequest.of(HttpMethod.POST, path));
  }

  private static final class RecordingServerCall extends ServerCall<String, String> {
    private final ServiceRequestContext expectedContext;
    private final CountDownLatch operationsLatch;
    private final List<String> operations = new CopyOnWriteArrayList<>();
    private final List<ServiceRequestContext> contexts = new CopyOnWriteArrayList<>();

    private RecordingServerCall(
      ServiceRequestContext expectedContext,
      int expectedOperations
    ) {
      this.expectedContext = expectedContext;
      this.operationsLatch = new CountDownLatch(expectedOperations);
    }

    @Override
    public void request(int messageCount) {
      record("request:" + messageCount);
    }

    @Override
    public void sendHeaders(Metadata headers) {
      record("headers");
    }

    @Override
    public void sendMessage(String message) {
      record("message:" + message);
    }

    @Override
    public void close(Status status, Metadata trailers) {
      record("close:" + status.getCode());
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public MethodDescriptor<String, String> getMethodDescriptor() {
      throw new UnsupportedOperationException();
    }

    private void record(String operation) {
      var currentContext = ServiceRequestContext.current();
      assertSame(expectedContext, currentContext);
      operations.add(operation);
      contexts.add(currentContext);
      operationsLatch.countDown();
    }

    private boolean await() throws InterruptedException {
      return operationsLatch.await(5, TimeUnit.SECONDS);
    }

    private List<String> operations() {
      return List.copyOf(operations);
    }

    private List<ServiceRequestContext> contexts() {
      return List.copyOf(contexts);
    }
  }
}
