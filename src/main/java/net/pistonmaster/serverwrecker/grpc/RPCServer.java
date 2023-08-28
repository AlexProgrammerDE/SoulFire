/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.grpc;

import ch.jalu.injector.Injector;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Key;
import java.util.concurrent.TimeUnit;

public class RPCServer {
    private static final Logger logger = LoggerFactory.getLogger(RPCServer.class);

    private final int port;
    private final Server server;

    public RPCServer(int port, Injector injector, Key jwtKey) {
        this(jwtKey, Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create()), port, injector);
    }

    public RPCServer(Key jwtKey, ServerBuilder<?> serverBuilder, int port, Injector injector) {
        this.port = port;
        server = serverBuilder
                .intercept(new ServerInterceptor() {
                    @Override
                    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                                 ServerCallHandler<ReqT, RespT> next) {
                        call.setCompression("gzip");
                        return next.startCall(call, headers);
                    }
                })
                .addService(injector.getSingleton(LogServiceImpl.class))
                .addService(injector.getSingleton(CommandServiceImpl.class))
                .intercept(new JwtServerInterceptor(jwtKey))
                .build();
    }

    public void start() throws IOException {
        server.start();
        logger.info("RPC Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                RPCServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdownNow().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}
