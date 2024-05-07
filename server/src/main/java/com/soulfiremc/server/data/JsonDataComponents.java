package com.soulfiremc.server.data;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.util.GsonInstance;
import java.util.HashMap;
import java.util.Map;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

public record JsonDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> components) {
  public static final TypeAdapter<JsonDataComponents> SERIALIZER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, JsonDataComponents value) {
      throw new UnsupportedOperationException("JsonDataComponents serialization is not supported");
    }

    @Override
    public JsonDataComponents read(JsonReader in) {
      var parsedJson = GsonInstance.GSON.<JsonObject>fromJson(in, JsonObject.class);
      var map = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
      for (var entry : parsedJson.entrySet()) {
        System.out.println(entry.getKey());
      }

      return new JsonDataComponents(map);
    }
  };
}
