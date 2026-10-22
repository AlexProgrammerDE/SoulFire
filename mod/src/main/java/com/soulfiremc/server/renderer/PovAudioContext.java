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

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.EXTThreadLocalContext;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;

/// Each bot owns an independent loopback device, bound only on its platform sound thread.
public final class PovAudioContext {
  private final long device;
  private final long context;
  private final ALCapabilities capabilities;
  private volatile boolean closed;

  public PovAudioContext(long device, long context) {
    this.device = device;
    this.context = context;
    this.capabilities = AL.getCapabilities();
  }

  public void run(Runnable task) {
    if (closed) return;
    if (Thread.currentThread().isVirtual()) {
      throw new IllegalStateException("POV audio requires the sound executor's platform thread");
    }
    var previous = EXTThreadLocalContext.alcGetThreadContext();
    var previousCapabilities = previous == 0 ? null : AL.getCapabilities();
    if (!EXTThreadLocalContext.alcSetThreadContext(context)) {
      throw new IllegalStateException("Could not bind the POV audio context");
    }
    AL.setCurrentThread(capabilities);
    try {
      task.run();
    } finally {
      EXTThreadLocalContext.alcSetThreadContext(previous);
      AL.setCurrentThread(previousCapabilities);
    }
  }

  public short[] render(int samples) {
    if (samples < 1 || samples > PovAudioCapture.SAMPLE_RATE / 10) {
      throw new IllegalArgumentException("Audio capture must be between one sample and 100 ms");
    }
    var result = new short[samples * PovAudioCapture.CHANNELS];
    run(() -> {
      try (var stack = MemoryStack.stackPush()) {
        var pcm = stack.mallocShort(result.length);
        SOFTLoopback.alcRenderSamplesSOFT(device, pcm, samples);
        pcm.get(result);
      }
    });
    return result;
  }

  public void invalidate() { closed = true; }

  public static void unbind() {
    EXTThreadLocalContext.alcSetThreadContext(0);
    AL.setCurrentThread(null);
  }
}
