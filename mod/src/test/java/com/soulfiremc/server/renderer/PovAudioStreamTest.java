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

import com.soulfiremc.mod.mixin.soulfire.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PovAudioStreamTest {
  @Test
  void failedAudioInitializationAndCleanupReturnAnErrorWithoutEndingVideoCapture() {
    var minecraft = mock(Minecraft.class);
    var manager = mock(SoundManager.class);
    var engine = mock(SoundEngine.class);
    var capture = (PovAudioCapture) engine;
    when(minecraft.getSoundManager()).thenReturn(manager);
    when(((SoundManagerAccessor) manager).soulfire$getSoundEngine()).thenReturn(engine);
    doThrow(new IllegalStateException()).when(capture).soulfire$setAudioEnabled(true);
    doThrow(new IllegalStateException()).when(capture).soulfire$setAudioEnabled(false);
    try (var stream = new PovAudioStream()) {
      var failed = stream.capture(minecraft, true, 0);
      assertFalse(failed.enabled());
      assertFalse(failed.error().isEmpty());
      assertTrue(failed.packets().isEmpty());
      var muted = stream.capture(minecraft, false, 20_000);
      assertFalse(muted.enabled());
      assertTrue(muted.error().isEmpty());
      assertTrue(muted.packets().isEmpty());
    }
    verify(capture).soulfire$setAudioEnabled(false);
  }
}
