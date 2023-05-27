/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.level.map.MapData;
import com.github.steveice10.mc.protocol.data.game.level.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.pistonmaster.serverwrecker.protocol.bot.utils.MapColorUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

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
            this.mapData = packet.getData();
            /*
            Path path = Path.of("mapdata-" + packet.getMapId() + ".png");
            BufferedImage image = MapColorUtils.generateFromData(this.mapData);

            try {
                ImageIO.write(image, "png", path.toFile());
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }

        System.out.println("MapDataState: " + this);
    }
}
