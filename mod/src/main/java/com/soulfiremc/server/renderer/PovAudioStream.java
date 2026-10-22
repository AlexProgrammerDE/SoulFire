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

import com.google.protobuf.ByteString;
import com.soulfiremc.grpc.generated.PovAudioPacket;
import com.soulfiremc.mod.mixin.soulfire.SoundManagerAccessor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/// Owned by one POV session; all capture and cleanup run on the bot's game thread.
@Slf4j
public final class PovAudioStream implements AutoCloseable {
  private PovAudioCapture capture;
  private PovAudioEncoder encoder;
  private boolean requested;
  private String error = "";
  private long nextTimestampUs;

  public Batch capture(Minecraft minecraft, boolean enabled, long timestampUs) {
    if (enabled != requested) {
      close();
      requested = enabled;
      error = "";
      if (enabled) {
        try {
          capture = (PovAudioCapture) ((SoundManagerAccessor) minecraft.getSoundManager()).soulfire$getSoundEngine();
          capture.soulfire$setAudioEnabled(true);
          encoder = new PovAudioEncoder();
          nextTimestampUs = timestampUs;
        } catch (RuntimeException failure) {
          log.warn("POV audio unavailable", failure);
          close();
          error = "Audio could not start on the server.";
        }
      }
    }
    if (encoder == null) return new Batch(List.of(), false, error);
    try {
      var remainingUs = timestampUs - nextTimestampUs;
      if (remainingUs > 100_000) {
        // A stalled viewer must not hear a backlog of old effects on recovery.
        minecraft.getSoundManager().stop();
        nextTimestampUs = timestampUs;
        return new Batch(List.of(), true, "");
      }
      minecraft.getSoundManager().updateSource(minecraft.gameRenderer.mainCamera());
      var packets = new ArrayList<PovAudioPacket>();
      while (nextTimestampUs + 20_000 <= timestampUs) {
        var pcm = capture.soulfire$renderAudio(PovAudioCapture.FRAME_SAMPLES);
        var data = encoder.encode(pcm);
        packets.add(PovAudioPacket.newBuilder()
          .setData(ByteString.copyFrom(data)).setTimestampUs(nextTimestampUs).build());
        nextTimestampUs += 20_000;
      }
      return new Batch(List.copyOf(packets), true, "");
    } catch (RuntimeException failure) {
      log.warn("POV audio capture failed", failure);
      close();
      error = "Audio stopped on the server. Mute and unmute to retry.";
      return new Batch(List.of(), false, error);
    }
  }

  @Override
  public void close() {
    var previousCapture = capture;
    capture = null;
    try {
      if (previousCapture != null) previousCapture.soulfire$setAudioEnabled(false);
    } catch (RuntimeException failure) {
      log.warn("Could not close POV audio capture", failure);
    }
    var previousEncoder = encoder;
    encoder = null;
    try {
      if (previousEncoder != null) previousEncoder.close();
    } catch (RuntimeException failure) {
      log.warn("Could not close POV audio encoder", failure);
    }
  }

  public record Batch(List<PovAudioPacket> packets, boolean enabled, String error) {}
}
