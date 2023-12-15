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
package net.pistonmaster.serverwrecker.server.pathfinding.controller;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.protocol.BotConnection;

@RequiredArgsConstructor
public class CollectBlockController {
    private final BlockType blockType;

    public void start(BotConnection botConnection) {

    }

    public void searchBlockWithinRadiusAndY(BotConnection botConnection, BlockType blockType, int radius, int minY, int maxY) {
        int radiusSquared = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radiusSquared) {
                        /*
                        Vector3i loc = playerLocation.clone().add(x, y, z);
                        if (loc.getBlock().getType() == blockType) {
                            player.sendMessage("Found " + blockType.toString() + " at: " + loc.toString());
                            // Do something with the found block
                        }
                         */
                    }
                }
            }
        }
    }
}
