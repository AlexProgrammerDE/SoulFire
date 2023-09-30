package net.pistonmaster.serverwrecker.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResourceData {
    public static final GlobalBlockPalette GLOBAL_BLOCK_PALETTE;
    public static final Map<String, String> MOJANG_TRANSLATIONS;

    // Static initialization allows us to preload this in a native image
    static {
        Gson gson = new Gson();

        // Load translations
        JsonObject translations;
        try (InputStream stream = ResourceData.class.getClassLoader().getResourceAsStream("minecraft/en_us.json")) {
            Objects.requireNonNull(stream, "en_us.json not found");
            translations = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Map<String, String> mojangTranslations = new HashMap<>();
        for (Map.Entry<String, JsonElement> translationEntry : translations.entrySet()) {
            mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
        }

        MOJANG_TRANSLATIONS = mojangTranslations;

        // Load block states
        JsonObject blocks;
        try (InputStream stream = ResourceData.class.getClassLoader().getResourceAsStream("minecraft/blocks.json")) {
            Objects.requireNonNull(stream, "blocks.json not found");
            blocks = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // Load global palette
        Map<Integer, BlockStateMeta> stateMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> blockEntry : blocks.entrySet()) {
            int i = 0;
            for (JsonElement state : blockEntry.getValue().getAsJsonObject().get("states").getAsJsonArray()) {
                stateMap.put(state.getAsJsonObject().get("id").getAsInt(), new BlockStateMeta(blockEntry.getKey(), i));
                i++;
            }
        }

        GLOBAL_BLOCK_PALETTE = new GlobalBlockPalette(stateMap.size());
        for (Map.Entry<Integer, BlockStateMeta> entry : stateMap.entrySet()) {
            GLOBAL_BLOCK_PALETTE.add(entry.getKey(), entry.getValue());
        }
    }
}
