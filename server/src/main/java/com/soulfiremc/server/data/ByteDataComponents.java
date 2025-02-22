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
package com.soulfiremc.server.data;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public record ByteDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> components) {
  public static final TypeAdapter<ByteDataComponents> SERIALIZER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, ByteDataComponents value) {
      throw new UnsupportedOperationException("ByteDataComponents serialization is not supported");
    }

    @Override
    public ByteDataComponents read(JsonReader in) {
      var parsedJson = GsonInstance.GSON.<JsonObject>fromJson(in, JsonObject.class);
      var map = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
      for (var entry : parsedJson.entrySet()) {
        var value = entry.getValue().getAsString();
        var bytes = Base64.getDecoder().decode(value);
        var buf = Unpooled.wrappedBuffer(bytes);
        var dataComponentType = DataComponentTypes.from(MinecraftTypes.readVarInt(buf));
        var dataComponent = dataComponentType.readDataComponent(buf);
        map.put(dataComponentType, dataComponent);
      }

      return new ByteDataComponents(map);
    }
  };
}
