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

import com.linecorp.armeria.server.ServiceRequestContext;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/// Keeps gRPC response operations inside the Armeria request context that
/// created their call.
///
/// A completion callback can run inline while another request is active. This
/// happens, for example, when one bot action replaces another and completes its
/// future. Armeria cannot push the completed call's context while the replacing
/// request's context is still current. Queueing the operation on the original
/// context's event loop breaks that reentrant call chain. Always queue operations
/// so a response in the current context cannot overtake previously queued headers.
public final class RequestContextResponseInterceptor implements ServerInterceptor {
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> serverCall,
    Metadata metadata,
    ServerCallHandler<ReqT, RespT> serverCallHandler
  ) {
    var context = ServiceRequestContext.current();
    return serverCallHandler.startCall(
      new ContextualServerCall<>(serverCall, context),
      metadata
    );
  }

  private static final class ContextualServerCall<ReqT, RespT>
    extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
    private final ServiceRequestContext context;

    private ContextualServerCall(
      ServerCall<ReqT, RespT> delegate,
      ServiceRequestContext context
    ) {
      super(delegate);
      this.context = context;
    }

    @Override
    public void request(int messageCount) {
      execute(() -> delegate().request(messageCount));
    }

    @Override
    public void sendHeaders(Metadata headers) {
      execute(() -> delegate().sendHeaders(headers));
    }

    @Override
    public void sendMessage(RespT message) {
      execute(() -> delegate().sendMessage(message));
    }

    @Override
    public void close(Status status, Metadata trailers) {
      execute(() -> delegate().close(status, trailers));
    }

    @Override
    public void setMessageCompression(boolean enabled) {
      execute(() -> delegate().setMessageCompression(enabled));
    }

    @Override
    public void setCompression(String compression) {
      execute(() -> delegate().setCompression(compression));
    }

    @Override
    public void setOnReadyThreshold(int messageCount) {
      execute(() -> delegate().setOnReadyThreshold(messageCount));
    }

    private void execute(Runnable operation) {
      context.eventLoop().execute(operation);
    }
  }
}
