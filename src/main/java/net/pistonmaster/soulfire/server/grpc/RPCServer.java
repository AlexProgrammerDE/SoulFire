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
package net.pistonmaster.soulfire.server.grpc;

import ch.jalu.injector.Injector;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RPCServer {
    @Getter
    private final String host;
    @Getter
    private final int port;
    private final Server server;

    public RPCServer(String host, int port, Injector injector, SecretKey jwtKey) {
        this(jwtKey, NettyServerBuilder.forAddress(new InetSocketAddress(host, port), InsecureServerCredentials.create()), host, port, injector);
    }

    public RPCServer(SecretKey jwtKey, ServerBuilder<?> serverBuilder, String host, int port, Injector injector) {
        this.host = host;
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
                .addService(injector.getSingleton(ConfigServiceImpl.class))
                .addService(injector.getSingleton(CommandServiceImpl.class))
                .addService(injector.getSingleton(AttackServiceImpl.class))
                .intercept(new JwtServerInterceptor(jwtKey))
                .build();
    }

    public void start() throws IOException {
        server.start();
        log.info("RPC Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("*** shutting down gRPC server since JVM is shutting down");
            try {
                shutdown();
            } catch (Throwable e) {
                log.error("Error while shutting down gRPC server", e);
                return;
            }
            log.info("*** server shut down");
        }));
    }

    public void shutdown() throws InterruptedException {
        if (!server.shutdown().awaitTermination(3, TimeUnit.SECONDS)
                && !server.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Unable to shutdown gRPC server");
        }
    }
}
