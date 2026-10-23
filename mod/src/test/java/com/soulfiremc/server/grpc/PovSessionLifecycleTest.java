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
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.common.logging.RequestLogAccess;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.ServiceRequestContextWrapper;
import com.mojang.blaze3d.platform.Window;
import com.soulfiremc.grpc.generated.PovFrame;
import com.soulfiremc.grpc.generated.PovWatchRequest;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotDisconnectedEvent;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.bot.PovInputController;
import com.soulfiremc.server.renderer.PovFrameTasks;
import com.soulfiremc.server.renderer.PovVideoEncoder;
import com.soulfiremc.server.renderer.VulkanRenderer;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.test.utils.TestBootstrap;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.kyori.adventure.text.Component;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class PovSessionLifecycleTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  private enum Cancellation { GRPC, CONNECTION, DISCONNECT }

  static Stream<Arguments> interruptions() {
    return Stream.of(true, false).flatMap(startup -> Stream.of(Cancellation.values()).map(cancellation -> Arguments.of(startup, cancellation)));
  }

  @ParameterizedTest
  @MethodSource("interruptions")
  void cancellationReleasesControlWhileEncoderWorkIsBlocked(boolean duringStartup, Cancellation cancellation) throws Exception {
    try (var fixture = new WatchFixture()) {
      fixture.watch();
      assertNull(fixture.terminalError.get());
      assertNotNull(fixture.cancel.get());
      assertNotNull(fixture.scheduled.get());
      var unrelated = mock(BotConnection.class);
      when(unrelated.accountProfileId()).thenReturn(fixture.botId);
      SoulFireAPI.postEvent(new BotDisconnectedEvent(unrelated, Component.text("Other connection epoch")));
      assertThrows(BotControlLeaseManager.InvalidLeaseException.class, () -> fixture.leases.authorize(fixture.botId, null));

      var enteredEncoder = new CountDownLatch(1);
      var resumeEncoder = new CountDownLatch(1);
      var image = mock(VulkanRenderer.RgbaFrame.class);
      when(image.width()).thenReturn(320);
      when(image.height()).thenReturn(180);
      when(image.pixels()).thenReturn(ByteBuffer.allocateDirect(4));
      var encoder = new AtomicReference<PovVideoEncoder>();
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var frame = executor.submit(() -> {
          try (var renderer = mockStatic(VulkanRenderer.class);
               var encoders = mockConstruction(PovVideoEncoder.class, (created, _) -> {
                 encoder.set(created);
                 if (duringStartup) { enteredEncoder.countDown(); assertTrue(resumeEncoder.await(5, TimeUnit.SECONDS)); }
                 when(created.encode(any(ByteBuffer.class), anyLong(), anyBoolean())).thenAnswer(_ -> {
                   if (!duringStartup) { enteredEncoder.countDown(); assertTrue(resumeEncoder.await(5, TimeUnit.SECONDS)); }
                   return new PovVideoEncoder.EncodedFrame(new byte[]{1}, 0, true, "avc1.42E01E");
                 });
               })) {
            renderer.when(() -> VulkanRenderer.renderInteractive(any(), anyInt(), anyInt(), anyInt(), anyInt(), any()))
              .thenReturn(image);
            fixture.scheduled.get().run();
          }
          return null;
        });
        try {
          assertTrue(enteredEncoder.await(5, TimeUnit.SECONDS));
          Runnable interrupt = switch (cancellation) {
            case GRPC -> fixture.cancel.get();
            case CONNECTION -> () -> fixture.transportClosed.setSuccess();
            case DISCONNECT -> () -> {
              when(fixture.bot.isDisconnecting()).thenReturn(true);
              SoulFireAPI.postEvent(new BotDisconnectedEvent(fixture.bot, Component.text("Bot stopped")));
            };
          };
          executor.submit(interrupt).get(1, TimeUnit.SECONDS);
          var nativeCleanup = fixture.pendingNative.poll();
          assertNotNull(nativeCleanup);
          nativeCleanup.run();
          verify(fixture.input).capture(false);
          if (cancellation == Cancellation.DISCONNECT) {
            doAnswer(_ -> { throw new IllegalStateException("Game loop stopped"); }).when(fixture.minecraft).execute(any(Runnable.class));
          }
          var replacement = fixture.leases.acquire(fixture.botId, fixture.owner, Duration.ofSeconds(10));
          resumeEncoder.countDown();
          frame.get(5, TimeUnit.SECONDS);
          fixture.drainNative();
          fixture.leases.authorize(fixture.botId, replacement.token());
          verify(fixture.input).capture(false);
          verify(image).close();
          verify(encoder.get()).close();
          verify(fixture.transportClosed).removeListener(any());
          fixture.cancel.get().run();
          fixture.leases.authorize(fixture.botId, replacement.token());
          if (duringStartup) verify(encoder.get(), never()).encode(any(ByteBuffer.class), anyLong(), anyBoolean());
          verify(fixture.observer, never()).onNext(any());
          if (cancellation == Cancellation.DISCONNECT) {
            assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(fixture.terminalError.get()).getCode());
            assertEquals(1, fixture.errors.get());
          } else assertEquals(0, fixture.errors.get());
        } finally {
          resumeEncoder.countDown();
          frame.get(5, TimeUnit.SECONDS);
        }
      }
    }
  }

  @Test
  void watchRejectsAConnectionWhoseShutdownAlreadyStarted() {
    try (var fixture = new WatchFixture()) {
      when(fixture.bot.isDisconnecting()).thenReturn(true);
      fixture.watch();
      assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(fixture.terminalError.get()).getCode());
      assertDoesNotThrow(() -> fixture.leases.authorize(fixture.botId, null));
      verify(fixture.scheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
  }

  @Test
  void disconnectBeforeListenerRegistrationStillClosesTheViewer() {
    try (var fixture = new WatchFixture()) {
      when(fixture.channel.closeFuture()).thenAnswer(_ -> {
        when(fixture.bot.isDisconnecting()).thenReturn(true);
        return fixture.transportClosed;
      });
      fixture.watch();
      assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(fixture.terminalError.get()).getCode());
      assertDoesNotThrow(() -> fixture.leases.authorize(fixture.botId, null));
      fixture.drainNative();
      verify(fixture.input).capture(false);
      fixture.scheduled.get().run();
      verify(fixture.captures, never()).submit(any());
      fixture.cancel.get().run();
      assertEquals(1, fixture.errors.get());
    }
  }

  private static final class WatchFixture implements AutoCloseable {
    final UUID instanceId = UUID.randomUUID();
    final UUID botId = UUID.randomUUID();
    final UUID owner = UUID.randomUUID();
    final SoulFireServer server = mock();
    final SoulFireScheduler scheduler = mock();
    final InstanceManager instance = mock();
    final BotConnection bot = mock();
    final BotControlLeaseManager leases = new BotControlLeaseManager();
    final SoulFireUser user = mock();
    final Minecraft minecraft = mock();
    final PovInputController input = mock();
    final PovFrameTasks captures = mock();
    final ConcurrentLinkedQueue<Runnable> pendingNative = new ConcurrentLinkedQueue<>();
    final AtomicReference<Runnable> scheduled = new AtomicReference<>();
    final AtomicReference<Runnable> cancel = new AtomicReference<>();
    final AtomicReference<Throwable> terminalError = new AtomicReference<>();
    final AtomicInteger errors = new AtomicInteger();
    final ServerCallStreamObserver<PovFrame> observer = mock();
    final Channel channel = mock();
    final DefaultChannelPromise transportClosed = spy(new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE));
    final ServiceRequestContext requestContext;
    final PovServiceImpl service = new PovServiceImpl(server);

    WatchFixture() {
      when(server.scheduler()).thenReturn(scheduler);
      when(bot.accountProfileId()).thenReturn(botId);
      when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
      when(instance.botConnections()).thenReturn(Map.of(botId, bot));
      when(instance.botControlLeaseManager()).thenReturn(leases);
      when(user.getUniqueId()).thenReturn(owner);
      when(bot.minecraft()).thenReturn(minecraft);
      minecraft.player = mock(LocalPlayer.class);
      minecraft.level = mock(ClientLevel.class);
      minecraft.gui = mock(Gui.class);
      minecraft.keyboardHandler = mock(KeyboardHandler.class);
      var window = mock(Window.class);
      window.currentCursor = mock();
      when(minecraft.getWindow()).thenReturn(window);
      when(bot.povInput()).thenReturn(input);
      doAnswer(call -> { pendingNative.add(call.getArgument(0)); return null; })
        .when(minecraft).execute(any(Runnable.class));
      when(bot.povFrameTasks()).thenReturn(captures);
      doAnswer(call -> CompletableFuture.completedFuture(call.<Supplier<?>>getArgument(0).get()))
        .when(captures).submit(any());
      doAnswer(call -> { scheduled.set(call.getArgument(0)); return null; })
        .when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
      when(observer.isReady()).thenReturn(true);
      doAnswer(call -> {
        errors.incrementAndGet();
        terminalError.set(call.getArgument(0));
        return null;
      }).when(observer).onError(any());
      doAnswer(call -> { cancel.set(call.getArgument(0)); return null; })
        .when(observer).setOnCancelHandler(any());
      when(channel.closeFuture()).thenReturn(transportClosed);
      var log = mock(RequestLogAccess.class);
      var partial = mock(RequestLog.class);
      when(log.partial()).thenReturn(partial);
      when(partial.channel()).thenReturn(channel);
      requestContext = new ServiceRequestContextWrapper(ServiceRequestContext.of(HttpRequest.of(HttpMethod.POST, "/pov"))) {
        @Override public ServiceRequestContext root() { return this; }
        @Override public RequestLogAccess log() { return log; }
      };
    }

    void watch() {
      try (var ignored = requestContext.push()) {
        Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() -> service.watch(
          PovWatchRequest.newBuilder().setInstanceId(instanceId.toString()).setBotId(botId.toString())
            .setSessionId(UUID.randomUUID().toString()).setWidth(320).setHeight(180).build(), observer));
      }
    }

    void drainNative() {
      for (Runnable task; (task = pendingNative.poll()) != null;) task.run();
    }

    @Override
    public void close() {
      if (cancel.get() != null) cancel.get().run();
      drainNative();
    }
  }
}
