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
package net.pistonmaster.soulfire.server.protocol.bot.utils;

import com.github.steveice10.mc.protocol.data.game.level.map.MapData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

/**
 * Big thanks to <a href="https://github.com/LB--/MCModify/blob/java/src/main/java/com/lb_stuff/mcmodify/minecraft/Map.java">MCModify</a>
 */
public class MapColorUtils {
    private static final Color[] MAP_COLORS;
    private static final IndexColorModel MAP_COLOR_MODEL;

    static {
        final var baseMapColors = MapColor.values();

        MAP_COLORS = new Color[baseMapColors.length * 4];
        for (var i = 0; i < baseMapColors.length; ++i) {
            var bc = baseMapColors[i].color;
            MAP_COLORS[i * 4] = generateShade(bc, 180.0);
            MAP_COLORS[i * 4 + 1] = generateShade(bc, 220.0);
            MAP_COLORS[i * 4 + 2] = bc; // 255, so no need to generate a shade
            MAP_COLORS[i * 4 + 3] = generateShade(bc, 135.0);
        }

        var r = new byte[MAP_COLORS.length];
        var g = new byte[MAP_COLORS.length];
        var b = new byte[MAP_COLORS.length];
        var a = new byte[MAP_COLORS.length];

        for (var i = 0; i < MAP_COLORS.length; ++i) {
            var mc = MAP_COLORS[i];
            r[i] = (byte) mc.getRed();
            g[i] = (byte) mc.getGreen();
            b[i] = (byte) mc.getBlue();
            a[i] = (byte) mc.getAlpha();
        }

        MAP_COLOR_MODEL = new IndexColorModel(8, MAP_COLORS.length, r, g, b, a);
    }

    private MapColorUtils() {
    }

    private static Color generateShade(Color c, double shade) {
        shade /= 255.0;
        return new Color((int) (c.getRed() * shade + 0.5), (int) (c.getGreen() * shade + 0.5), (int) (c.getBlue() * shade + 0.5), c.getAlpha());
    }

    public static BufferedImage generateFromData(MapData mapData) {
        var width = mapData.getColumns();
        var height = mapData.getRows();
        var image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, MAP_COLOR_MODEL);
        for (var i = 0; i < width; ++i) {
            for (var j = 0; j < height; ++j) {
                var c = MAP_COLORS[mapData.getData()[i + j * width]];
                image.setRGB(i, j, c.getRGB());
            }
        }
        return image;
    }

    /**
     * Keep in sync with: <a href="https://minecraft.wiki/w/Map_item_format#Color_table">Minecraft Wiki Page</a>
     */
    private enum MapColor {
        NONE(0, 0, 0),
        GRASS(127, 178, 56),
        SAND(247, 233, 163),
        WOOL(199, 199, 199),
        FIRE(255, 0, 0),
        ICE(160, 160, 255),
        METAL(167, 167, 167),
        PLANT(0, 124, 0),
        SNOW(255, 255, 255),
        CLAY(164, 168, 184),
        DIRT(151, 109, 77),
        STONE(112, 112, 112),
        WATER(64, 64, 255),
        WOOD(143, 119, 72),
        QUARTZ(255, 252, 245),
        COLOR_ORANGE(216, 127, 51),
        COLOR_MAGENTA(178, 76, 216),
        COLOR_LIGHT_BLUE(102, 153, 216),
        COLOR_YELLOW(229, 229, 51),
        COLOR_LIGHT_GREEN(127, 204, 25),
        COLOR_PINK(242, 127, 165),
        COLOR_GRAY(76, 76, 76),
        COLOR_LIGHT_GRAY(153, 153, 153),
        COLOR_CYAN(76, 127, 153),
        COLOR_PURPLE(127, 63, 178),
        COLOR_BLUE(51, 76, 178),
        COLOR_BROWN(102, 76, 51),
        COLOR_GREEN(102, 127, 51),
        COLOR_RED(153, 51, 51),
        COLOR_BLACK(25, 25, 25),
        GOLD(250, 238, 77),
        DIAMOND(92, 219, 213),
        LAPIS(74, 128, 255),
        EMERALD(0, 217, 58),
        PODZOL(129, 86, 49),
        NETHER(112, 2, 0),
        TERRACOTTA_WHITE(209, 177, 161),
        TERRACOTTA_ORANGE(159, 82, 36),
        TERRACOTTA_MAGENTA(149, 87, 108),
        TERRACOTTA_LIGHT_BLUE(112, 108, 138),
        TERRACOTTA_YELLOW(186, 133, 36),
        TERRACOTTA_LIGHT_GREEN(103, 117, 53),
        TERRACOTTA_PINK(160, 77, 78),
        TERRACOTTA_GRAY(57, 41, 35),
        TERRACOTTA_LIGHT_GRAY(135, 107, 98),
        TERRACOTTA_CYAN(87, 92, 92),
        TERRACOTTA_PURPLE(122, 73, 88),
        TERRACOTTA_BLUE(76, 62, 92),
        TERRACOTTA_BROWN(76, 50, 35),
        TERRACOTTA_GREEN(76, 82, 42),
        TERRACOTTA_RED(142, 60, 46),
        TERRACOTTA_BLACK(37, 22, 16),
        CRIMSON_NYLIUM(189, 48, 49),
        CRIMSON_STEM(148, 63, 97),
        CRIMSON_HYPHAE(92, 25, 29),
        WARPED_NYLIUM(22, 126, 134),
        WARPED_STEM(58, 142, 140),
        WARPED_HYPHAE(86, 44, 62),
        WARPED_WART_BLOCK(20, 180, 133),
        DEEPSLATE(100, 100, 100),
        RAW_IRON(216, 175, 147),
        GLOW_LICHEN(127, 167, 150);

        private final Color color;

        MapColor(int r, int g, int b) {
            this.color = new Color(r, g, b);
        }
    }
}
