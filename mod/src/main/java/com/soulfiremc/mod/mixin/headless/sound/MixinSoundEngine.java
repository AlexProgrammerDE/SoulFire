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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.DeviceTracker;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.PollingDeviceTracker;
import com.soulfiremc.mod.mixin.soulfire.AudioLibraryAccessor;
import com.soulfiremc.server.renderer.PovAudioCapture;
import com.soulfiremc.server.renderer.PovAudioContext;
import com.soulfiremc.server.renderer.PovAudioExecutor;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine implements PovAudioCapture {
  @Shadow @Final private Library library;
  @Shadow @Final private SoundEngineExecutor executor;
  @Shadow private boolean loaded;
  @Shadow protected abstract void loadLibrary();
  @Shadow public abstract void destroy();
  @Unique private final Object soulfire$audioLock = new Object();
  @Unique private boolean soulfire$audioRequested;
  @Unique private PovAudioContext soulfire$audioContext;

  @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Library;createDeviceTracker()Lcom/mojang/blaze3d/audio/DeviceTracker;"))
  private DeviceTracker createDeviceTracker() {
    return new PollingDeviceTracker(DeviceList.EMPTY);
  }

  @Override
  public void soulfire$setAudioEnabled(boolean enabled) {
    soulfire$audioRequested = enabled;
    if (enabled) {
      loadLibrary();
      if (!loaded) throw new IllegalStateException("POV audio could not initialize");
    } else {
      destroy();
    }
  }

  @Override
  public boolean soulfire$isAudioEnabled() { return loaded; }

  @Override
  public short[] soulfire$renderAudio(int samples) {
    synchronized (soulfire$audioLock) {
      var context = soulfire$audioContext;
      if (!loaded || context == null) return new short[0];
      return executor.submit(() -> context.render(samples)).join();
    }
  }

  @WrapMethod(method = "loadLibrary")
  private void loadAudibleLibrary(Operation<Void> original) {
    if (!soulfire$audioRequested || loaded) return;
    original.call();
    if (!loaded && soulfire$audioContext != null) {
      try {
        ((PovAudioExecutor) executor).soulfire$runAudio(library::cleanup);
      } finally {
        soulfire$audioContext = null;
        ((PovAudioExecutor) executor).soulfire$shutdownAudio();
      }
    }
  }

  @Redirect(method = "loadLibrary", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Library;init(Ljava/lang/String;Lcom/mojang/blaze3d/audio/DeviceList;Z)V"))
  private void initializeAudioOnSoundThread(Library soundLibrary, String preferred, DeviceList devices, boolean hrtf) {
    soulfire$audioContext = ((PovAudioExecutor) executor).soulfire$initializeAudio(() -> {
      soundLibrary.init(preferred, devices, hrtf);
      var access = (AudioLibraryAccessor) soundLibrary;
      return new PovAudioContext(access.soulfire$getDevice(), access.soulfire$getContext());
    });
  }

  @Redirect(method = "loadLibrary", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;reset()V"))
  private void resetListenerOnSoundThread(Listener listener) {
    ((PovAudioExecutor) executor).soulfire$runAudio(listener::reset);
  }

  @WrapMethod(method = "destroy")
  private void destroyAudibleLibrary(Operation<Void> original) {
    synchronized (soulfire$audioLock) {
      if (soulfire$audioContext == null) { original.call(); return; }
      try {
        ((PovAudioExecutor) executor).soulfire$runAudio(() -> original.call());
      } finally {
        soulfire$audioContext = null;
        ((PovAudioExecutor) executor).soulfire$shutdownAudio();
      }
    }
  }

  @WrapMethod(method = "stopAll")
  private void stopAudibleSounds(Operation<Void> original) {
    var context = soulfire$audioContext;
    if (context == null) original.call();
    else ((PovAudioExecutor) executor).soulfire$runAudio(() -> original.call());
  }

  @Redirect(method = "stopAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngineExecutor;shutDown()V"))
  private void keepAudioThreadForCleanup(SoundEngineExecutor soundExecutor) {
    if (soulfire$audioContext == null) soundExecutor.shutDown();
  }

  @Redirect(method = "stopAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngineExecutor;startUp()V"))
  private void restartAudioThread(SoundEngineExecutor soundExecutor) {
    if (soulfire$audioContext == null) soundExecutor.startUp();
  }

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void tickOnlyAudible(boolean paused, CallbackInfo ci) {
    if (!loaded) ci.cancel();
  }

  @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
  private void delayOnlyAudible(SoundInstance instance, int delay, CallbackInfo ci) {
    if (!loaded) ci.cancel();
  }

  @Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
  private void queueOnlyAudible(TickableSoundInstance instance, CallbackInfo ci) {
    if (!loaded) ci.cancel();
  }

  @Inject(method = "requestPreload", at = @At("HEAD"), cancellable = true)
  private void loadSoundsOnDemand(Sound sound, CallbackInfo ci) { ci.cancel(); }

  @Inject(method = "shouldChangeDevice", at = @At("HEAD"), cancellable = true)
  private void keepLoopbackDevice(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }
}
