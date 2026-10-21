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
package com.soulfiremc.mod;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.Options;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

final class HeadlessSoundEngineTest {
  @Test
  void constructingSoundEngineDoesNotQueryNativeAudioDevices() {
    try (var library = mockStatic(Library.class)) {
      library.when(Library::createDeviceTracker).thenThrow(new AssertionError("Headless audio must not query host devices"));

      var engine = new SoundEngine(mock(SoundManager.class), mock(Options.class), mock(ResourceProvider.class));

      assertTrue(engine.getAvailableSoundDevices().isEmpty());
      library.verify(Library::createDeviceTracker, never());
    }
  }
}
