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

import com.soulfiremc.grpc.generated.ScriptEvent;
import io.grpc.stub.ServerCallStreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/// Owns each monitoring connection independently for one script activation.
@Slf4j
final class ScriptEventSubscribers {
  private final Set<Subscription> subscriptions = new LinkedHashSet<>();
  private ScriptEvent terminalEvent;

  synchronized void add(ServerCallStreamObserver<ScriptEvent> observer) {
    var subscription = new Subscription(observer);
    observer.setOnCancelHandler(() -> remove(subscription));
    observer.setOnCloseHandler(() -> remove(subscription));
    if (subscription.disconnected || observer.isCancelled()) {
      return;
    }
    if (terminalEvent != null) {
      write(subscription, terminalEvent, true);
    } else {
      subscriptions.add(subscription);
    }
  }

  synchronized void send(Supplier<ScriptEvent> eventSupplier) {
    if (terminalEvent != null || subscriptions.isEmpty()) {
      return;
    }
    var event = eventSupplier.get();
    for (var subscription : List.copyOf(subscriptions)) {
      write(subscription, event, false);
    }
  }

  synchronized void sendTo(ServerCallStreamObserver<ScriptEvent> observer, ScriptEvent event) {
    for (var subscription : List.copyOf(subscriptions)) {
      if (subscription.observer == observer) {
        write(subscription, event, false);
      }
    }
  }

  synchronized void complete(ScriptEvent event) {
    if (terminalEvent != null) {
      return;
    }
    terminalEvent = event;
    var current = List.copyOf(subscriptions);
    subscriptions.clear();
    for (var subscription : current) {
      write(subscription, event, true);
    }
  }

  private synchronized void remove(Subscription subscription) {
    subscription.disconnected = true;
    subscriptions.remove(subscription);
  }

  private void write(Subscription subscription, ScriptEvent event, boolean complete) {
    if (subscription.disconnected || subscription.observer.isCancelled()) {
      remove(subscription);
      return;
    }
    try {
      subscription.observer.onNext(event);
      if (complete && !subscription.disconnected && !subscription.observer.isCancelled()) {
        subscription.observer.onCompleted();
      }
    } catch (RuntimeException e) {
      remove(subscription);
      log.debug("Error sending script event", e);
    } finally {
      if (complete) {
        remove(subscription);
      }
    }
  }

  private static final class Subscription {
    private final ServerCallStreamObserver<ScriptEvent> observer;
    private boolean disconnected;

    private Subscription(ServerCallStreamObserver<ScriptEvent> observer) {
      this.observer = observer;
    }
  }
}
