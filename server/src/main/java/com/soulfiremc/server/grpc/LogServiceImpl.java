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

import com.soulfiremc.grpc.generated.LogRequest;
import com.soulfiremc.grpc.generated.LogResponse;
import com.soulfiremc.grpc.generated.LogsServiceGrpc;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.system.SystemLogEvent;
import com.soulfiremc.server.user.Permissions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
  @Getter
  private final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();
  private final QueueWithMaxSize<String> logs = new QueueWithMaxSize<>(300); // Keep max 300 logs

  public LogServiceImpl() {
    SoulFireAPI.registerListener(SystemLogEvent.class, event -> logs.add(event.message()));
  }

  @Override
  public void subscribe(LogRequest request, StreamObserver<LogResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.SUBSCRIBE_LOGS);

    try {
      var sender = new ConnectionMessageSender((ServerCallStreamObserver<LogResponse>) responseObserver);
      subscribers.put(ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(), sender);

      sendPreviousLogs(request.getPrevious(), sender);
      SoulFireAPI.registerListener(SystemLogEvent.class, e -> sender.sendMessage(e.message()));
    } catch (Throwable t) {
      log.error("Error subscribing to logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private void sendPreviousLogs(int requestPrevious, ConnectionMessageSender sender) {
    for (var log : logs.getNewest(requestPrevious)) {
      sender.sendMessage(log);
    }
  }

  private record ConnectionMessageSender(ServerCallStreamObserver<LogResponse> responseObserver) {
    public void sendMessage(String message) {
      if (responseObserver.isCancelled()) {
        return;
      }

      responseObserver.onNext(LogResponse.newBuilder().setMessage(message).build());
    }
  }

  public static class QueueWithMaxSize<E> {
    private final int maxSize;
    private final Queue<E> queue;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public QueueWithMaxSize(int maxSize) {
      this.maxSize = maxSize;
      this.queue = new ArrayBlockingQueue<>(maxSize);
    }

    public boolean add(E element) {
      lock.writeLock().lock();
      try {
        if (queue.size() >= maxSize) {
          queue.poll(); // Remove the oldest element if max size is reached
        }

        return queue.add(element);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public List<E> getNewest(int amount) {
      if (amount > maxSize) {
        throw new IllegalArgumentException("Amount is bigger than max size!");
      }

      lock.readLock().lock();
      try {
        var list = new ArrayList<>(queue);
        var size = list.size();
        var start = size - amount;

        if (start < 0) {
          start = 0;
        }

        return list.subList(start, size);
      } finally {
        lock.readLock().unlock();
      }
    }
  }
}
