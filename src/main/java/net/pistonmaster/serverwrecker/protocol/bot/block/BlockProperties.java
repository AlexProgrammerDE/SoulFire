package net.pistonmaster.serverwrecker.protocol.bot.block;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.*;

public class BlockProperties {
    private final Object2BooleanMap<String> booleanProperties;
    private final Object2IntMap<String> intProperties;
    private final Object2ObjectMap<String, String> stringProperties;

    public BlockProperties(JsonObject properties) {
        this.booleanProperties = new Object2BooleanArrayMap<>();
        this.intProperties = new Object2IntArrayMap<>();
        this.stringProperties = new Object2ObjectArrayMap<>();

        for (var property : properties.entrySet()) {
            var key = property.getKey();
            var value = property.getValue().toString();

            if (value.equals("true") || value.equals("false")) {
                booleanProperties.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("-?\\d+")) {
                intProperties.put(key, Integer.parseInt(value));
            } else {
                stringProperties.put(key, value);
            }
        }
    }

    public boolean getBoolean(String key) {
        return booleanProperties.getBoolean(key);
    }

    public int getInt(String key) {
        return intProperties.getInt(key);
    }

    public String getString(String key) {
        return stringProperties.get(key);
    }
}
