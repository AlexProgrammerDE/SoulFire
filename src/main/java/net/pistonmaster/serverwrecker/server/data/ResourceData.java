/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.server.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockState;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockStateProperties;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.GlobalBlockPalette;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceData {
    public static final GlobalBlockPalette GLOBAL_BLOCK_PALETTE;
    public static final Map<String, String> MOJANG_TRANSLATIONS;
    public static final Int2ObjectMap<BlockStateProperties> BLOCK_STATE_PROPERTIES;
    public static final IntSet BLOCK_STATE_DEFAULTS;
    public static final Reference2ObjectMap<BlockType, List<BlockState>> BLOCK_STATES;

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
        try (var stream = ResourceData.class.getClassLoader().getResourceAsStream("minecraft/block_states.json")) {
            Objects.requireNonNull(stream, "block_states.json not found");
            blocks = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var blockStateProperties = new Int2ObjectOpenHashMap<BlockStateProperties>();
        var blockStateDefaults = new IntArraySet();
        for (var blockEntry : blocks.entrySet()) {
            for (var state : blockEntry.getValue().getAsJsonObject().getAsJsonArray("states")) {
                var stateObject = state.getAsJsonObject();
                var stateId = stateObject.get("id").getAsInt();
                if (stateObject.get("default") != null) {
                    blockStateDefaults.add(stateId);
                }

                blockStateProperties.put(stateId, new BlockStateProperties(stateObject.getAsJsonObject("properties")));
            }
        }

        BLOCK_STATE_PROPERTIES = blockStateProperties;
        BLOCK_STATE_DEFAULTS = blockStateDefaults;

        // Load global palette
        Int2ObjectMap<BlockState> stateMap = new Int2ObjectOpenHashMap<>();
        Reference2ObjectMap<BlockType, List<BlockState>> blockStates = new Reference2ObjectOpenHashMap<>();
        for (var blockEntry : blocks.entrySet()) {
            var name = blockEntry.getKey();
            var blockEntryData = blockEntry.getValue().getAsJsonObject();
            var i = 0;
            for (var state : blockEntryData.getAsJsonArray("states")) {
                var stateObject = state.getAsJsonObject();
                var stateId = stateObject.get("id").getAsInt();
                var stateInstance = new BlockState(
                        stateId,
                        Objects.requireNonNull(BlockType.getByName(name), "BlockType was null!"),
                        i
                );
                stateMap.put(stateId, stateInstance);
                blockStates.computeIfAbsent(stateInstance.blockType(), (key) -> new ObjectArrayList<>())
                        .add(stateInstance);

                i++;
            }
        }

        GLOBAL_BLOCK_PALETTE = new GlobalBlockPalette(stateMap);
        BLOCK_STATES = blockStates;

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
