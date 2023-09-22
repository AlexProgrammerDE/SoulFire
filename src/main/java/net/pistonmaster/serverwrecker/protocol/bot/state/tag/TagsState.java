package net.pistonmaster.serverwrecker.protocol.bot.state.tag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.pistonmaster.serverwrecker.ServerWrecker;

import java.util.Map;

public class TagsState {
    public void handleTagData(ServerWrecker serverWrecker, Map<String, Map<String, int[]>> tags) {
        for (Map.Entry<String, Map<String, int[]>> registry : tags.entrySet()) {
            JsonObject registryJson = serverWrecker.getTagData().getAsJsonObject(stripMinecraft(registry.getKey()));

            for (Map.Entry<String, int[]> tag : registry.getValue().entrySet()) {
                JsonArray tagJson = registryJson.getAsJsonArray(stripMinecraft(tag.getKey()));
                for (int i : tag.getValue()) {
                    // System.out.println(tagJson.get(i));
                }
            }
        }
    }

    private static String stripMinecraft(String input) {
        return input.replace("minecraft:", "");
    }
}
