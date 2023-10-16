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
package net.pistonmaster.serverwrecker.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;

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

        Map<String, String> mojangTranslations = new HashMap<>();
        for (var translationEntry : translations.entrySet()) {
            mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
        }

        MOJANG_TRANSLATIONS = mojangTranslations;

        // Load block states
        JsonObject blocks;
        try (var stream = ResourceData.class.getClassLoader().getResourceAsStream("minecraft/blocks.json")) {
            Objects.requireNonNull(stream, "blocks.json not found");
            blocks = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // Load global palette
        Int2ObjectMap<BlockStateMeta> stateMap = new Int2ObjectOpenHashMap<>();
        for (var blockEntry : blocks.entrySet()) {
            var i = 0;
            for (var state : blockEntry.getValue().getAsJsonObject().get("states").getAsJsonArray()) {
                stateMap.put(state.getAsJsonObject().get("id").getAsInt(), new BlockStateMeta(blockEntry.getKey(), i));
                i++;
            }
        }

        GLOBAL_BLOCK_PALETTE = new GlobalBlockPalette(stateMap);

        // Initialize all classes
        doNothing(BlockItems.VALUES);
        doNothing(BlockShapeType.VALUES);
        doNothing(BlockStateLoader.BLOCK_SHAPES);
        doNothing(BlockType.VALUES);
        doNothing(EntityType.VALUES);
        doNothing(ItemType.VALUES);
        doNothing(new MinecraftGraph(null));
    }

    @SuppressWarnings("unused")
    private static void doNothing(Object param) {
        // Do nothing
    }
}
