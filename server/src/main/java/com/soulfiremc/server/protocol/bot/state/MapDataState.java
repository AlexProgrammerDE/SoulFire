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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.MapColor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapData;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapIcon;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;

import java.awt.image.BufferedImage;

@Setter
@Getter
@ToString
public class MapDataState {
  private final byte[] colorData = new byte[128 * 128];
  private final byte scale;
  private final boolean locked;
  private MapIcon[] icons;

  public MapDataState(ClientboundMapItemDataPacket packet) {
    this.scale = packet.getScale();
    this.locked = packet.isLocked();
  }

  private static int convertABGRToARGB(int color) {
    var first = (color >> 16) & 0xFF;
    var second = color & 0xFF;
    return (color & 0xFF00FF00) | (second << 16) | first;
  }

  public void update(ClientboundMapItemDataPacket packet) {
    this.icons = packet.getIcons();

    if (packet.getData() != null) {
      this.mergeIntoMap(packet.getData());
    }
  }

  private void mergeIntoMap(MapData source) {
    var width = source.getColumns();
    var height = source.getRows();

    var xOffset = source.getX();
    var yOffset = source.getY();
    for (var relativeX = 0; relativeX < width; ++relativeX) {
      for (var relativeY = 0; relativeY < height; ++relativeY) {
        setColor(
          xOffset + relativeX,
          yOffset + relativeY,
          source.getData()[relativeX + relativeY * width]);
      }
    }
  }

  public byte getColor(int x, int y) {
    return colorData[x + y * 128];
  }

  public void setColor(int x, int y, byte color) {
    colorData[x + y * 128] = color;
  }

  public BufferedImage toBufferedImage() {
    var image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
    for (var x = 0; x < 128; ++x) {
      for (var y = 0; y < 128; ++y) {
        image.setRGB(x, y, convertABGRToARGB(MapColor.getColorFromPackedId(getColor(x, y))));
      }
    }

    return image;
  }
}
