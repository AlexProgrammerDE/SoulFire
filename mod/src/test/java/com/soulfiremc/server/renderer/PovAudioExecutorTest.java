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
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.EXTThreadLocalContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PovAudioExecutorTest {
  @Test
  void virtualBotKeepsNativeInitializationPlaybackAndCleanupOnOnePlatformThread() throws Exception {
    var completed = new CompletableFuture<Void>();
    Thread.ofVirtual().start(() -> {
      try {
        var executor = (PovAudioExecutor) new SoundEngineExecutor();
        var owner = new AtomicReference<Thread>();
        for (var cycle = 0; cycle < 3; cycle++) {
          var library = new Library();
          var context = executor.soulfire$initializeAudio(() -> {
            assertFalse(Thread.currentThread().isVirtual());
            owner.set(Thread.currentThread());
            library.init(null, DeviceList.EMPTY, false);
            var access = (AudioLibraryAccessor) library;
            return new PovAudioContext(access.soulfire$getDevice(), access.soulfire$getContext());
          });
          try {
            for (var sample = 0; sample < 5; sample++) {
              executor.soulfire$runAudio(() -> {
                assertSame(owner.get(), Thread.currentThread());
                var nativeContext = EXTThreadLocalContext.alcGetThreadContext();
                assertNotEquals(0, nativeContext);
                // Blocking here must never migrate OpenAL onto another carrier thread.
                CompletableFuture.runAsync(() -> {}).join();
                assertEquals(nativeContext, EXTThreadLocalContext.alcGetThreadContext());
                assertEquals(PovAudioCapture.FRAME_SAMPLES * 2, context.render(PovAudioCapture.FRAME_SAMPLES).length);
                executor.soulfire$runAudio(() -> assertSame(owner.get(), Thread.currentThread()));
              });
            }
          } finally {
            try { executor.soulfire$runAudio(library::cleanup); }
            finally { executor.soulfire$shutdownAudio(); }
          }
          assertThrows(IllegalStateException.class, () -> executor.soulfire$runAudio(() -> {}));
        }
        completed.complete(null);
      } catch (Throwable failure) {
        completed.completeExceptionally(failure);
      }
    });
    completed.get(20, TimeUnit.SECONDS);
  }
}
