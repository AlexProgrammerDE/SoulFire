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

/// Minecraft sound engines expose loopback capture only while an audible viewer owns the bot.
public interface PovAudioCapture {
  int SAMPLE_RATE = 48_000;
  int CHANNELS = 2;
  int FRAME_SAMPLES = 960;

  void soulfire$setAudioEnabled(boolean enabled);

  boolean soulfire$isAudioEnabled();

  short[] soulfire$renderAudio(int samples);
}
