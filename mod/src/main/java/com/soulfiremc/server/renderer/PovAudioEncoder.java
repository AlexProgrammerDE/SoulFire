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

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;

import java.nio.ByteOrder;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_frame;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_copy;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_get_buffer;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_make_writable;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

/// Fixed 20 ms stereo Opus packets, with no file container or unbounded encoder queue.
public final class PovAudioEncoder implements AutoCloseable {
  private AVCodecContext context;
  private AVFrame frame;
  private AVPacket packet;
  private long samples;

  public PovAudioEncoder() {
    try {
      var codec = avcodec_find_encoder_by_name("libopus");
      if (codec == null || codec.isNull()) throw new IllegalStateException("Opus encoder is unavailable");
      context = avcodec_alloc_context3(codec);
      context.sample_rate(PovAudioCapture.SAMPLE_RATE).sample_fmt(AV_SAMPLE_FMT_S16).bit_rate(96_000);
      context.time_base().num(1).den(PovAudioCapture.SAMPLE_RATE);
      av_channel_layout_default(context.ch_layout(), PovAudioCapture.CHANNELS);
      var options = new AVDictionary(null);
      try {
        av_dict_set(options, "application", "lowdelay", 0);
        av_dict_set(options, "frame_duration", "20", 0);
        check(avcodec_open2(context, codec, options), "Opening Opus encoder");
      } finally {
        av_dict_free(options);
      }
      frame = av_frame_alloc();
      frame.format(context.sample_fmt()).sample_rate(context.sample_rate()).nb_samples(PovAudioCapture.FRAME_SAMPLES);
      check(av_channel_layout_copy(frame.ch_layout(), context.ch_layout()), "Setting audio channels");
      check(av_frame_get_buffer(frame, 0), "Allocating audio samples");
      packet = av_packet_alloc();
    } catch (Throwable error) {
      close();
      throw error;
    }
  }

  public byte[] encode(short[] pcm) {
    if (context == null) throw new IllegalStateException("Audio encoder is closed");
    if (pcm.length != PovAudioCapture.FRAME_SAMPLES * PovAudioCapture.CHANNELS) {
      throw new IllegalArgumentException("An Opus frame requires 20 ms of stereo samples");
    }
    check(av_frame_make_writable(frame), "Preparing audio samples");
    frame.data(0).capacity((long) pcm.length * Short.BYTES).asByteBuffer()
      .order(ByteOrder.nativeOrder()).asShortBuffer().put(pcm);
    frame.pts(samples);
    samples += PovAudioCapture.FRAME_SAMPLES;
    check(avcodec_send_frame(context, frame), "Encoding audio samples");
    check(avcodec_receive_packet(context, packet), "Receiving Opus packet");
    try {
      var data = new byte[packet.size()];
      packet.data().get(data);
      return data;
    } finally {
      av_packet_unref(packet);
    }
  }

  @Override
  public void close() {
    if (context != null && !context.isNull() && packet != null && !packet.isNull()) {
      avcodec_send_frame(context, null);
      while (avcodec_receive_packet(context, packet) >= 0) av_packet_unref(packet);
    }
    if (packet != null && !packet.isNull()) { av_packet_free(packet); packet = null; }
    if (frame != null && !frame.isNull()) { av_frame_free(frame); frame = null; }
    if (context != null && !context.isNull()) { avcodec_free_context(context); context = null; }
  }

  private static void check(int result, String operation) {
    if (result >= 0) return;
    try (var message = new BytePointer(256)) {
      av_strerror(result, message, 256);
      throw new IllegalStateException(operation + ": " + message.getString());
    }
  }
}
