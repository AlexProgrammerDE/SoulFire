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
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.bot.PovInputController;
import com.soulfiremc.server.renderer.PovFrameTasks;
import com.soulfiremc.server.renderer.PovVideoEncoder;
import com.soulfiremc.server.renderer.VulkanRenderer;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.test.utils.TestBootstrap;
import io.grpc.Context;
import io.grpc.stub.ServerCallStreamObserver;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  static Stream<Arguments> interruptions() {
    return Stream.of(true, false).flatMap(startup -> Stream.of(true, false).map(connection -> Arguments.of(startup, connection)));
  }

  @ParameterizedTest
  @MethodSource("interruptions")
  void cancellationReleasesControlWhileEncoderWorkIsBlocked(boolean duringStartup, boolean connectionClosed) throws Exception {
    var server = mock(SoulFireServer.class);
    var scheduler = mock(SoulFireScheduler.class);
    when(server.scheduler()).thenReturn(scheduler);
    var instance = mock(InstanceManager.class);
    var instanceId = UUID.randomUUID();
    var botId = UUID.randomUUID();
    var owner = UUID.randomUUID();
    var bot = mock(BotConnection.class);
    when(bot.accountProfileId()).thenReturn(botId);
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.botConnections()).thenReturn(Map.of(botId, bot));
    var leases = new BotControlLeaseManager();
    when(instance.botControlLeaseManager()).thenReturn(leases);
    var user = mock(SoulFireUser.class);
    when(user.getUniqueId()).thenReturn(owner);
    var minecraft = mock(Minecraft.class);
    when(bot.minecraft()).thenReturn(minecraft);
    minecraft.player = mock(LocalPlayer.class);
    minecraft.level = mock(ClientLevel.class);
    minecraft.gui = mock(Gui.class);
    minecraft.keyboardHandler = mock(KeyboardHandler.class);
    var window = mock(Window.class);
    window.currentCursor = mock();
    when(minecraft.getWindow()).thenReturn(window);
    var input = mock(PovInputController.class);
    when(bot.povInput()).thenReturn(input);
    var pendingNative = new ConcurrentLinkedQueue<Runnable>();
    doAnswer(call -> { pendingNative.add(call.getArgument(0)); return null; })
      .when(minecraft).execute(any(Runnable.class));
    var captures = mock(PovFrameTasks.class);
    when(bot.povFrameTasks()).thenReturn(captures);
    doAnswer(call -> CompletableFuture.completedFuture(call.<Supplier<?>>getArgument(0).get()))
      .when(captures).submit(any());
    var scheduled = new AtomicReference<Runnable>();
    doAnswer(call -> { scheduled.set(call.getArgument(0)); return null; })
      .when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    ServerCallStreamObserver<PovFrame> observer = mock();
    doAnswer(call -> { throw new AssertionError(call.<Throwable>getArgument(0)); }).when(observer).onError(any());
    when(observer.isReady()).thenReturn(true);
    var cancel = new AtomicReference<Runnable>();
    doAnswer(call -> { cancel.set(call.getArgument(0)); return null; })
      .when(observer).setOnCancelHandler(any());
    var channel = mock(Channel.class);
    var transportClosed = spy(new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE));
    when(channel.closeFuture()).thenReturn(transportClosed);
    var log = mock(RequestLogAccess.class);
    var partial = mock(RequestLog.class);
    when(log.partial()).thenReturn(partial);
    when(partial.channel()).thenReturn(channel);
    var requestContext = new ServiceRequestContextWrapper(ServiceRequestContext.of(HttpRequest.of(HttpMethod.POST, "/pov"))) {
      @Override public ServiceRequestContext root() { return this; }
      @Override public RequestLogAccess log() { return log; }
    };
    var service = new PovServiceImpl(server);
    try (var ignored = requestContext.push()) {
      Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() -> service.watch(
        PovWatchRequest.newBuilder().setInstanceId(instanceId.toString()).setBotId(botId.toString())
          .setSessionId(UUID.randomUUID().toString()).setWidth(320).setHeight(180).build(), observer));
    }
    assertNotNull(cancel.get());
    assertNotNull(scheduled.get());
    assertThrows(BotControlLeaseManager.InvalidLeaseException.class, () -> leases.authorize(botId, null));

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
          scheduled.get().run();
        }
        return null;
      });
      try {
        assertTrue(enteredEncoder.await(5, TimeUnit.SECONDS));
        executor.submit(connectionClosed ? () -> transportClosed.setSuccess() : cancel.get()).get(1, TimeUnit.SECONDS);
        var nativeCleanup = pendingNative.poll();
        assertNotNull(nativeCleanup);
        nativeCleanup.run();
        verify(input).capture(false);
        var replacement = leases.acquire(botId, owner, Duration.ofSeconds(10));
        resumeEncoder.countDown();
        frame.get(5, TimeUnit.SECONDS);
        for (Runnable task; (task = pendingNative.poll()) != null;) task.run();
        leases.authorize(botId, replacement.token());
        verify(input).capture(false);
        verify(image).close();
        verify(encoder.get()).close();
        verify(transportClosed).removeListener(any());
        cancel.get().run();
        leases.authorize(botId, replacement.token());
        if (duringStartup) verify(encoder.get(), never()).encode(any(ByteBuffer.class), anyLong(), anyBoolean());
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any());
      } finally {
        resumeEncoder.countDown();
        frame.get(5, TimeUnit.SECONDS);
      }
    }
  }
}
