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

@Mixin(SoundEngineExecutor.class)
public class MixinSoundEngineExecutor implements PovAudioExecutor {
  @Unique
  private PovAudioContext soulfire$audioContext;

  @Override
  public void soulfire$setAudioContext(PovAudioContext context) {
    soulfire$audioContext = context;
  }

  @Redirect(method = "createThread", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"))
  private void startAudioThread(Thread thread) {
    if (soulfire$audioContext != null) thread.start();
  }

  @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
  private void bindAudioContext(Runnable task, CallbackInfoReturnable<Runnable> cir) {
    var context = soulfire$audioContext;
    cir.setReturnValue(context == null ? () -> {} : () -> context.run(task));
  }
}
