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

import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.Library;
import com.soulfiremc.mod.mixin.soulfire.AudioLibraryAccessor;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class PovAudioContextTest {
  @Test
  void failedDeviceCapabilitiesReleaseTheDeviceAndAllowRetry() {
    assertInitializationFailureIsCleanedUp(false);
  }

  @Test
  void failedContextCapabilitiesReleaseNativeHandlesAndAllowRetry() {
    assertInitializationFailureIsCleanedUp(true);
  }

  private static void assertInitializationFailureIsCleanedUp(boolean afterContextCreation) {
    var library = new Library();
    var access = (AudioLibraryAccessor) library;
    var closedDevices = new AtomicInteger();
    var destroyedContexts = new AtomicInteger();
    var failOnce = new AtomicBoolean(true);
    var failure = new IllegalStateException("Injected capability failure");
    try (var devices = mockStatic(ALC10.class, CALLS_REAL_METHODS);
         var deviceCapabilities = mockStatic(ALC.class, CALLS_REAL_METHODS);
         var capabilities = mockStatic(AL.class, CALLS_REAL_METHODS)) {
      devices.when(() -> ALC10.alcCloseDevice(anyLong())).thenAnswer(invocation -> {
        var closed = (boolean) invocation.callRealMethod();
        if (closed) closedDevices.incrementAndGet();
        return closed;
      });
      devices.when(() -> ALC10.alcDestroyContext(anyLong())).thenAnswer(invocation -> {
        invocation.callRealMethod();
        destroyedContexts.incrementAndGet();
        return null;
      });
      if (afterContextCreation) {
        capabilities.when(() -> AL.createCapabilities(any(ALCCapabilities.class))).thenAnswer(invocation -> {
          if (failOnce.getAndSet(false)) throw failure;
          return invocation.callRealMethod();
        });
      } else {
        deviceCapabilities.when(() -> ALC.createCapabilities(anyLong())).thenAnswer(invocation -> {
          if (failOnce.getAndSet(false)) throw failure;
          return invocation.callRealMethod();
        });
      }
      try {
        for (var cycle = 0; cycle < 2; cycle++) {
          failOnce.set(true);
          assertSame(failure, assertThrows(IllegalStateException.class, () -> library.init(null, DeviceList.EMPTY, false)));
          assertEquals(cycle * 2 + 1, closedDevices.get());
          assertEquals(cycle * (afterContextCreation ? 2 : 1) + (afterContextCreation ? 1 : 0), destroyedContexts.get());
          assertEquals(0, access.soulfire$getDevice());
          assertEquals(0, access.soulfire$getContext());
          library.init(null, DeviceList.EMPTY, false);
          var context = new PovAudioContext(access.soulfire$getDevice(), access.soulfire$getContext());
          assertEquals(PovAudioCapture.FRAME_SAMPLES * 2, context.render(PovAudioCapture.FRAME_SAMPLES).length);
          library.cleanup();
          context.invalidate();
          assertEquals(0, access.soulfire$getDevice());
          assertEquals(0, access.soulfire$getContext());
        }
      } finally {
        if (access.soulfire$getContext() != 0) library.cleanup();
        else if (access.soulfire$getDevice() != 0) ALC10.alcCloseDevice(access.soulfire$getDevice());
        PovAudioContext.unbind();
      }
    }
  }

  @Test
  void mixingOneBotNeverPlaysInAnotherBotsLoopbackDevice() {
    var first = new Library();
    var second = new Library();
    PovAudioContext firstContext = null;
    PovAudioContext secondContext = null;
    try {
      first.init(null, DeviceList.EMPTY, false);
      var firstAccess = (AudioLibraryAccessor) first;
      firstContext = new PovAudioContext(firstAccess.soulfire$getDevice(), firstAccess.soulfire$getContext());
      PovAudioContext.unbind();
      second.init(null, DeviceList.EMPTY, false);
      var secondAccess = (AudioLibraryAccessor) second;
      secondContext = new PovAudioContext(secondAccess.soulfire$getDevice(), secondAccess.soulfire$getContext());
      PovAudioContext.unbind();
      var source = new int[1];
      var buffer = new int[1];
      firstContext.run(() -> {
        try (var stack = MemoryStack.stackPush()) {
          var tone = stack.mallocShort(PovAudioCapture.FRAME_SAMPLES);
          for (var i = 0; i < tone.capacity(); i++) tone.put(i, (short) (12_000 * Math.sin(2 * Math.PI * 440 * i / PovAudioCapture.SAMPLE_RATE)));
          buffer[0] = AL10.alGenBuffers();
          source[0] = AL10.alGenSources();
          AL10.alBufferData(buffer[0], AL10.AL_FORMAT_MONO16, tone, PovAudioCapture.SAMPLE_RATE);
          AL10.alSourcei(source[0], AL10.AL_BUFFER, buffer[0]);
          AL10.alSourcePlay(source[0]);
        }
      });
      try {
        assertTrue(peak(firstContext.render(PovAudioCapture.FRAME_SAMPLES)) > 1_000);
        assertTrue(peak(secondContext.render(PovAudioCapture.FRAME_SAMPLES)) <= 1);
      } finally {
        firstContext.run(() -> {
          AL10.alDeleteSources(source[0]);
          AL10.alDeleteBuffers(buffer[0]);
        });
      }
    } finally {
      if (secondContext != null) { secondContext.run(second::cleanup); secondContext.invalidate(); }
      if (firstContext != null) { firstContext.run(first::cleanup); firstContext.invalidate(); }
    }
  }

  private static int peak(short[] samples) {
    var peak = 0;
    for (var sample : samples) peak = Math.max(peak, Math.abs((int) sample));
    return peak;
  }
}
