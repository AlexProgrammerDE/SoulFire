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

import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.junit.jupiter.api.Test;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_OPUS;
import static org.bytedeco.ffmpeg.global.avcodec.av_new_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PovAudioEncoderTest {
  @Test
  void opusPacketsDecodeImmediatelyAndPreserveIndependentStereoSignals() {
    var codec = avcodec_find_decoder(AV_CODEC_ID_OPUS);
    var decoder = avcodec_alloc_context3(codec);
    var packet = av_packet_alloc();
    var frame = av_frame_alloc();
    var output = new float[2][20 * PovAudioCapture.FRAME_SAMPLES];
    try (var encoder = new PovAudioEncoder()) {
      decoder.sample_rate(PovAudioCapture.SAMPLE_RATE);
      av_channel_layout_default(decoder.ch_layout(), 2);
      assertEquals(0, avcodec_open2(decoder, codec, (AVDictionary) null));
      for (var chunk = 0; chunk < 20; chunk++) {
        var pcm = new short[PovAudioCapture.FRAME_SAMPLES * 2];
        for (var sample = 0; sample < PovAudioCapture.FRAME_SAMPLES; sample++) {
          var time = (chunk * PovAudioCapture.FRAME_SAMPLES + sample) / (double) PovAudioCapture.SAMPLE_RATE;
          pcm[sample * 2] = (short) (12_000 * Math.sin(2 * Math.PI * 440 * time));
          pcm[sample * 2 + 1] = (short) (12_000 * Math.sin(2 * Math.PI * 880 * time));
        }
        var data = encoder.encode(pcm);
        assertTrue(data.length > 0 && data.length < 1024);
        assertEquals(0, av_new_packet(packet, data.length));
        packet.data().put(data);
        assertEquals(0, avcodec_send_packet(decoder, packet));
        assertEquals(0, avcodec_receive_frame(decoder, frame));
        assertEquals(PovAudioCapture.FRAME_SAMPLES, frame.nb_samples());
        assertEquals(AV_SAMPLE_FMT_FLTP, frame.format());
        for (var channel = 0; channel < 2; channel++) {
          frame.data(channel).capacity((long) frame.nb_samples() * Float.BYTES).asByteBuffer()
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            .get(output[channel], chunk * PovAudioCapture.FRAME_SAMPLES, frame.nb_samples());
        }
        av_packet_unref(packet);
        av_frame_unref(frame);
      }
      assertTrue(magnitude(output[0], 440) > 10 * magnitude(output[0], 880));
      assertTrue(magnitude(output[1], 880) > 10 * magnitude(output[1], 440));
      assertTrue(magnitude(output[0], 440) > 0.1);
      assertTrue(magnitude(output[1], 880) > 0.1);
    } finally {
      av_frame_free(frame);
      av_packet_free(packet);
      avcodec_free_context(decoder);
    }
  }

  private static double magnitude(float[] samples, double frequency) {
    var sine = 0.0;
    var cosine = 0.0;
    for (var i = 960; i < samples.length; i++) {
      var phase = 2 * Math.PI * frequency * i / PovAudioCapture.SAMPLE_RATE;
      sine += samples[i] * Math.sin(phase);
      cosine += samples[i] * Math.cos(phase);
    }
    return 2 * Math.hypot(sine, cosine) / (samples.length - 960);
  }
}
