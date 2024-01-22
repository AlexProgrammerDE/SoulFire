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
package net.pistonmaster.soulfire.server.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.level.map.MapData;
import com.github.steveice10.mc.protocol.data.game.level.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class MapDataState {
    private byte scale;
    private boolean locked;
    private MapIcon[] icons;
    private MapData mapData;

    public void update(ClientboundMapItemDataPacket packet) {
        this.scale = packet.getScale();
        this.locked = packet.isLocked();
        this.icons = packet.getIcons();

        if (packet.getData() != null) {
            if (this.mapData == null) {
                this.mapData = new MapData(128, 128, 0, 0, new byte[128 * 128]);
            }

            this.mergeIntoMap(packet.getData());
        }
    }

    private void mergeIntoMap(MapData source) {
        var width = source.getColumns();
        var height = source.getRows();

        var xOffset = source.getX();
        var yOffset = source.getY();
        for (var i = 0; i < width; ++i) {
            for (var j = 0; j < height; ++j) {
                var colorData = source.getData()[i + j * width];

                var x = xOffset + i;
                var y = yOffset + j;
                this.mapData.getData()[x + y * 128] = colorData;
            }
        }
    }
}
