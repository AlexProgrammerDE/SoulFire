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
package com.soulfiremc.mod.mixin.headless.sound;

import com.soulfiremc.server.renderer.PovAudioContext;
import com.soulfiremc.server.renderer.PovAudioExecutor;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(SoundEngineExecutor.class)
public class MixinSoundEngineExecutor implements PovAudioExecutor {
  @Unique
  private volatile PovAudioContext soulfire$audioContext;
  @Unique
  private volatile boolean soulfire$initializing;

  @Override
  public PovAudioContext soulfire$initializeAudio(Supplier<PovAudioContext> initializer) {
    var executor = (SoundEngineExecutor) (Object) this;
    soulfire$initializing = true;
    try {
      executor.startUp();
      return executor.submit(() -> {
        try {
          soulfire$audioContext = initializer.get();
          return soulfire$audioContext;
        } finally {
          PovAudioContext.unbind();
        }
      }).join();
    } catch (RuntimeException | Error failure) {
      executor.shutDown();
      throw failure;
    } finally {
      soulfire$initializing = false;
    }
  }

  @Override
  public void soulfire$runAudio(Runnable task) {
    if (soulfire$audioContext == null) throw new IllegalStateException("POV audio is not initialized");
    ((SoundEngineExecutor) (Object) this).submit(task).join();
  }

  @Override
  public void soulfire$shutdownAudio() {
    try {
      ((SoundEngineExecutor) (Object) this).shutDown();
    } finally {
      var context = soulfire$audioContext;
      soulfire$audioContext = null;
      if (context != null) context.invalidate();
    }
  }

  @Redirect(method = "createThread", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"))
  private void startAudioThread(Thread thread) {
    if (soulfire$initializing || soulfire$audioContext != null) thread.start();
  }

  @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
  private void bindAudioContext(Runnable task, CallbackInfoReturnable<Runnable> cir) {
    var context = soulfire$audioContext;
    if (context != null) cir.setReturnValue(() -> context.run(task));
    else cir.setReturnValue(soulfire$initializing ? task : () -> {});
  }
}
