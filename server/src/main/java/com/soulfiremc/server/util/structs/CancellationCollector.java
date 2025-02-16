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
package com.soulfiremc.server.util.structs;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CancellationCollector {
  private final List<CompletableFuture<?>> futures = new ArrayList<>();
  @Getter
  private boolean cancelled = false;

  public CancellationCollector(StreamObserver<?> casted) {
    var observer = (ServerCallStreamObserver<?>) casted;
    observer.setOnCancelHandler(this::cancelAll);
  }

  public synchronized <T> CompletableFuture<T> add(CompletableFuture<T> future) {
    if (cancelled) {
      future.cancel(true);

      // Just return a fake future that is already cancelled
      var fakeFuture = new CompletableFuture<T>();
      fakeFuture.cancel(true);
      return fakeFuture;
    }

    futures.add(future);
    return future;
  }

  public synchronized void cancelAll() {
    if (cancelled) {
      return;
    }

    cancelled = true;
    for (var future : futures) {
      future.cancel(true);
    }
  }
}
