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
package net.pistonmaster.soulfire.server.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.soulfire.server.protocol.bot.block.GlobalBlockPalette;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResourceData {
    public static final GlobalBlockPalette GLOBAL_BLOCK_PALETTE;
    public static final Map<String, String> MOJANG_TRANSLATIONS;

    // Static initialization allows us to preload this in a native image
    static {
        var gson = new Gson();

        // Load translations
        JsonObject translations;
        try (var stream = ResourceData.class.getClassLoader().getResourceAsStream("minecraft/en_us.json")) {
            Objects.requireNonNull(stream, "en_us.json not found");
            translations = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var mojangTranslations = new HashMap<String, String>();
        for (var translationEntry : translations.entrySet()) {
            mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
        }

        MOJANG_TRANSLATIONS = mojangTranslations;

        // Load global palette
        var stateMap = new Int2ObjectOpenHashMap<BlockState>();
        for (var blockEntry : BlockType.FROM_ID.values()) {
            for (var state : blockEntry.statesData().possibleStates()) {
                stateMap.put(state.id(), state);
            }
        }

        GLOBAL_BLOCK_PALETTE = new GlobalBlockPalette(stateMap);

        // Initialize all classes
        doNothing(BlockItems.VALUES);
        doNothing(BlockShapeGroup.FROM_ID);
        doNothing(BlockShapeLoader.BLOCK_SHAPES);
        doNothing(BlockType.FROM_ID);
        doNothing(EntityType.FROM_ID);
        doNothing(ItemType.FROM_ID);
        doNothing(new MinecraftGraph(null));
    }

    @SuppressWarnings("unused")
    private static void doNothing(Object param) {
        // Do nothing
    }
}
