/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.protocol;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;

public class SFProtocolHelper {
  private SFProtocolHelper() {}

  public static void writeChunkSection(
    ByteBuf buf, ChunkSection chunkSection, MinecraftCodecHelper codecHelper) {
    buf.writeShort(chunkSection.getBlockCount());

    codecHelper.writeDataPalette(buf, chunkSection.getChunkData());
    codecHelper.writeDataPalette(buf, chunkSection.getBiomeData());
  }

  public static ChunkSection readChunkSection(ByteBuf buf, MinecraftCodecHelper codecHelper) throws IOException {
    int blockCount = buf.readShort();

    var chunkPalette = codecHelper.readDataPalette(buf, PaletteType.CHUNK);
    var biomePalette = codecHelper.readDataPalette(buf, PaletteType.BIOME);
    return new ChunkSection(blockCount, chunkPalette, biomePalette);
  }
}
