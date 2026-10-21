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

import com.mojang.blaze3d.audio.Library;
import com.soulfiremc.server.renderer.PovAudioCapture;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.EXTThreadLocalContext;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(Library.class)
public class MixinLibrary {
  @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Library;openDeviceOrFallback(Ljava/lang/String;Ljava/lang/String;)J"))
  private long openLoopback(String preferred, String fallback) {
    var device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
    if (device == 0) throw new IllegalStateException("OpenAL loopback is unavailable");
    return device;
  }

  @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
  private void loopbackFormat(MemoryStack stack, boolean hrtf, CallbackInfoReturnable<IntBuffer> cir) {
    cir.setReturnValue(stack.ints(
      ALC10.ALC_FREQUENCY, PovAudioCapture.SAMPLE_RATE,
      SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, SOFTLoopback.ALC_STEREO_SOFT,
      SOFTLoopback.ALC_FORMAT_TYPE_SOFT, SOFTLoopback.ALC_SHORT_SOFT,
      0));
  }

  @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC;createCapabilities(J)Lorg/lwjgl/openal/ALCCapabilities;"))
  private ALCCapabilities requireThreadContexts(long device) {
    var capabilities = ALC.createCapabilities(device);
    if (!capabilities.ALC_EXT_thread_local_context) {
      ALC10.alcCloseDevice(device);
      throw new IllegalStateException("POV audio requires thread-local OpenAL contexts");
    }
    return capabilities;
  }

  @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcMakeContextCurrent(J)Z"))
  private boolean bindThreadContext(long context) {
    return EXTThreadLocalContext.alcSetThreadContext(context);
  }
}
