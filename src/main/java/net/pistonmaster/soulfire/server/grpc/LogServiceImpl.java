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

import io.grpc.stub.StreamObserver;
import net.pistonmaster.soulfire.grpc.generated.LogRequest;
import net.pistonmaster.soulfire.grpc.generated.LogResponse;
import net.pistonmaster.soulfire.grpc.generated.LogsServiceGrpc;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.system.SystemLogEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

public class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
    private final QueueWithMaxSize<String> logs = new QueueWithMaxSize<>(300); // Keep max 300 logs

    public LogServiceImpl() {
        SoulFireAPI.registerListener(SystemLogEvent.class, event ->
                logs.add(event.message()));
    }

    private static void publishLine(String line, StreamObserver<LogResponse> responseObserver) {
        var response = LogResponse.newBuilder()
                .setMessage(line)
                .build();

        responseObserver.onNext(response);
    }

    @Override
    public void subscribe(LogRequest request, StreamObserver<LogResponse> responseObserver) {
        sendPreviousLogs(request.getPrevious(), responseObserver);
        SoulFireAPI.registerListener(SystemLogEvent.class, new LogEventListener(responseObserver));
    }

    private void sendPreviousLogs(int requestPrevious, StreamObserver<LogResponse> responseObserver) {
        for (var log : logs.getNewest(requestPrevious)) {
            publishLine(log, responseObserver);
        }
    }

    private record LogEventListener(
            StreamObserver<LogResponse> responseObserver) implements Consumer<SystemLogEvent> {
        @Override
        public void accept(SystemLogEvent event) {
            publishLine(event.message(), responseObserver);
        }
    }

    public static class QueueWithMaxSize<E> {
        private final int maxSize;
        private final Queue<E> queue;

        public QueueWithMaxSize(int maxSize) {
            this.maxSize = maxSize;
            this.queue = new ArrayBlockingQueue<>(maxSize);
        }

        public synchronized boolean add(E element) {
            if (queue.size() >= maxSize) {
                queue.poll(); // Remove the oldest element if max size is reached
            }

            return queue.add(element);
        }

        public synchronized List<E> getNewest(int amount) {
            if (amount > maxSize) {
                throw new IllegalArgumentException("Amount is bigger than max size!");
            }

            var list = new ArrayList<>(queue);
            var size = list.size();
            var start = size - amount;

            if (start < 0) {
                start = 0;
            }

            return list.subList(start, size);
        }
    }
}
