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
package com.soulfiremc.server.protocol.bot.state.registry;

import com.soulfiremc.server.data.RegistryValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.lenni0451.mcstructs.nbt.io.NbtIO;
import net.lenni0451.mcstructs.nbt.io.NbtReadTracker;
import net.lenni0451.mcstructs.nbt.tags.CompoundTag;
import net.lenni0451.mcstructs.snbt.SNbtSerializer;
import net.lenni0451.mcstructs.text.serializer.TextComponentCodec;
import net.lenni0451.mcstructs.text.serializer.v1_20_3.json.JsonStyleSerializer_v1_20_3;
import net.lenni0451.mcstructs.text.serializer.v1_20_3.json.JsonTextSerializer_v1_20_3;
import net.lenni0451.mcstructs.text.serializer.v1_20_3.nbt.NbtStyleSerializer_v1_20_3;
import net.lenni0451.mcstructs.text.serializer.v1_20_3.nbt.NbtTextSerializer_v1_20_3;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.jetbrains.annotations.Nullable;

@Getter
public class ChatType implements RegistryValue<ChatType> {
  private static final SNbtSerializer<CompoundTag> SNBT_SERIALIZER = SNbtSerializer.V1_14;
  private static final NbtStyleSerializer_v1_20_3 NBT_STYLE_SERIALIZER = new NbtStyleSerializer_v1_20_3(TextComponentCodec.V1_20_3, new NbtTextSerializer_v1_20_3(TextComponentCodec.V1_20_3, SNBT_SERIALIZER), SNBT_SERIALIZER);
  private static final JsonStyleSerializer_v1_20_3 JSON_STYLE_SERIALIZER = new JsonStyleSerializer_v1_20_3(TextComponentCodec.V1_20_3, new JsonTextSerializer_v1_20_3(TextComponentCodec.V1_20_3, SNBT_SERIALIZER), SNBT_SERIALIZER);
  private final Key key;
  private final int id;
  private final String translationKey;
  private final List<String> parameters;
  private final Style style;

  @SneakyThrows
  public ChatType(Key key, int id, NbtMap chatTypeData) {
    this.key = key;
    this.id = id;

    var chat = chatTypeData.getCompound("chat");
    this.translationKey = chat.getString("translation_key");
    this.parameters = chat.getList("parameters", NbtType.STRING);
    if (chat.containsKey("style", NbtType.COMPOUND)) {
      var styleTag = chat.getCompound("style");
      var output = new ByteArrayOutputStream();
      new NBTOutputStream(new DataOutputStream(output)).writeValue(styleTag, 512);
      var lenniStyleNbt = NBT_STYLE_SERIALIZER.deserialize(NbtIO.JAVA.getReader().readCompound(new DataInputStream(new ByteArrayInputStream(output.toByteArray())), NbtReadTracker.unlimited()));
      var lenniStyleJson = JSON_STYLE_SERIALIZER.serialize(lenniStyleNbt);
      this.style = GsonComponentSerializer.gson().serializer().fromJson(lenniStyleJson, Style.class);
    } else{
      this.style = Style.empty();
    }
  }

  public TranslatableComponent buildComponent(BoundChatMessageInfo chatInfo) {
    var translationArgs = new ArrayList<ComponentLike>();
    for (var parameter : this.parameters) {
      switch (parameter) {
        case "content" -> translationArgs.add(chatInfo.content);
        case "sender" -> translationArgs.add(chatInfo.sender);
        case "target" -> translationArgs.add(Objects.requireNonNullElse(chatInfo.target, Component.empty()));
        default -> throw new IllegalArgumentException("Unknown parameter type: " + parameter);
      }
    }

    return Component.translatable(translationKey, null, style, translationArgs);
  }

  public record BoundChatMessageInfo(Component content, Component sender, @Nullable Component target) {
  }
}
