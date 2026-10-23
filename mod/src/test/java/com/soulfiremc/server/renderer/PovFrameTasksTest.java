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
package com.soulfiremc.server.renderer;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PovFrameTasksTest {
  @Test
  void captureReadsSimulationStateAtRenderPhase() {
    var state = new AtomicInteger(0);
    try (var tasks = new PovFrameTasks()) {
      var capture = tasks.submit(state::get);
      assertFalse(capture.isDone());
      state.set(1);
      tasks.drain(() -> {});
      assertEquals(1, capture.join());
    }
  }

  @Test
  void deferReentrantTasksAndCancelPendingCapturesOnClose() {
    var tasks = new PovFrameTasks();
    var nested = tasks.submit(() -> tasks.submit(() -> 42));
    tasks.drain(() -> {});
    assertFalse(nested.join().isDone());
    tasks.close();
    assertTrue(nested.join().isCancelled());
    assertTrue(tasks.submit(() -> 1).isCancelled());
  }

  @Test
  void failedCaptureDoesNotPreventTheNextCapture() {
    try (var tasks = new PovFrameTasks()) {
      var failure = tasks.submit(() -> { throw new IllegalStateException(); });
      var next = tasks.submit(() -> 42);
      tasks.drain(() -> {});
      assertTrue(failure.isCompletedExceptionally());
      assertEquals(42, next.join());
    }
  }

  @Test
  void activeCapturesWakeTheVanillaIdleLimiterAndEmptyFramesRemainIdle() {
    var minecraft = mock(Minecraft.class);
    minecraft.level = mock(ClientLevel.class);
    when(minecraft.getWindow()).thenReturn(mock(Window.class));
    var options = mock(Options.class, RETURNS_DEEP_STUBS);
    when(options.framerateLimit().get()).thenReturn(120);
    when(options.inactivityFpsLimit().get()).thenReturn(InactivityFpsLimit.AFK);
    try (var clock = mockStatic(Util.class); var tasks = new PovFrameTasks()) {
      clock.when(Util::getMillis).thenReturn(600_001L);
      var limiter = new FramerateLimitTracker(options, minecraft);
      assertEquals(10, limiter.getFramerateLimit());
      var capture = tasks.submit(limiter::getFramerateLimit);
      tasks.drain(limiter::onInputReceived);
      assertEquals(120, capture.join());

      clock.when(Util::getMillis).thenReturn(1_200_002L);
      tasks.drain(limiter::onInputReceived);
      assertEquals(10, limiter.getFramerateLimit());
    }
  }
}
