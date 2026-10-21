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

import com.soulfiremc.grpc.generated.NodeStarted;
import com.soulfiremc.grpc.generated.ScriptCompleted;
import com.soulfiremc.grpc.generated.ScriptEvent;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

final class ScriptEventSubscribersTest {
  private final ScriptEventSubscribers subscribers = new ScriptEventSubscribers();
  private final ServerCallStreamObserver<ScriptEvent> first = mock();
  private final ServerCallStreamObserver<ScriptEvent> second = mock();
  private final ScriptEvent event = ScriptEvent.newBuilder().setNodeStarted(NodeStarted.getDefaultInstance()).build();
  private final ScriptEvent terminal = ScriptEvent.newBuilder().setScriptCompleted(ScriptCompleted.getDefaultInstance()).build();

  @Test
  void disconnectingAnOlderViewerDoesNotDisconnectTheNewerViewer() {
    var cancel = new AtomicReference<Runnable>();
    doAnswer(call -> { cancel.set(call.getArgument(0)); return null; }).when(first).setOnCancelHandler(any());
    subscribers.add(first);
    subscribers.add(second);
    subscribers.send(() -> event);
    cancel.get().run();
    subscribers.send(() -> event);

    verify(first, times(1)).onNext(event);
    verify(second, times(2)).onNext(event);
    verify(second, never()).onCompleted();
  }

  @Test
  void cancellationDuringRegistrationCannotRetainAViewer() {
    doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; }).when(first).setOnCancelHandler(any());
    subscribers.add(first);
    subscribers.send(() -> event);
    subscribers.complete(terminal);

    verify(first, never()).onNext(any());
    verify(first, never()).onCompleted();
  }

  @Test
  void completionClosesEachViewerOnceAndCompletesLateViewers() {
    subscribers.add(first);
    subscribers.complete(terminal);
    subscribers.complete(terminal);
    subscribers.send(() -> event);
    subscribers.add(second);

    verify(first, times(1)).onNext(terminal);
    verify(first, times(1)).onCompleted();
    verify(second, times(1)).onNext(terminal);
    verify(second, times(1)).onCompleted();
    verify(first, never()).onNext(event);
  }

  @Test
  void reentrantDisconnectDoesNotInterruptOtherViewers() {
    var cancel = new AtomicReference<Runnable>();
    doAnswer(call -> { cancel.set(call.getArgument(0)); return null; }).when(first).setOnCloseHandler(any());
    doAnswer(_ -> { cancel.get().run(); return null; }).when(first).onNext(any());
    subscribers.add(first);
    subscribers.add(second);
    subscribers.send(() -> event);
    subscribers.send(() -> event);

    verify(first, times(1)).onNext(event);
    verify(second, times(2)).onNext(event);
  }

  @Test
  void failingViewerCannotInterruptOtherViewers() {
    doThrow(new IllegalStateException()).when(first).onNext(any());
    subscribers.add(first);
    subscribers.add(second);
    subscribers.send(() -> event);
    subscribers.send(() -> event);
    subscribers.complete(terminal);

    verify(first, times(1)).onNext(any());
    verify(second, times(2)).onNext(event);
    verify(second, times(1)).onCompleted();
  }

  @Test
  void unattendedScriptsDoNotBuildStreamingPayloads() {
    java.util.function.Supplier<ScriptEvent> supplier = mock();
    subscribers.send(supplier);
    subscribers.complete(terminal);
    subscribers.send(supplier);

    verify(supplier, never()).get();
  }
}
