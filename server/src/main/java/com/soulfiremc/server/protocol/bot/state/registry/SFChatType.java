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
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;

@Getter
public class SFChatType implements RegistryValue<SFChatType> {
  private static final SNbtSerializer<CompoundTag> SNBT_SERIALIZER = SNbtSerializer.V1_14;
  private static final NbtStyleSerializer_v1_20_3 NBT_STYLE_SERIALIZER = new NbtStyleSerializer_v1_20_3(TextComponentCodec.V1_20_3, new NbtTextSerializer_v1_20_3(TextComponentCodec.V1_20_3, SNBT_SERIALIZER), SNBT_SERIALIZER);
  private static final JsonStyleSerializer_v1_20_3 JSON_STYLE_SERIALIZER = new JsonStyleSerializer_v1_20_3(TextComponentCodec.V1_20_3, new JsonTextSerializer_v1_20_3(TextComponentCodec.V1_20_3, SNBT_SERIALIZER), SNBT_SERIALIZER);
  private final Key key;
  private final int id;
  private final ChatType mcplChatType;

  public SFChatType(Key key, int id, NbtMap chatTypeData) {
    this.key = key;
    this.id = id;

    var chat = chatTypeData.getCompound("chat");
    var narration = chatTypeData.getCompound("narration");
    this.mcplChatType = new ChatType(
      readDecoration(chat),
      readDecoration(narration)
    );
  }

  public SFChatType(ChatType chatType) {
    // Custom chat type from holder
    this.key = null;
    this.id = 0;
    this.mcplChatType = chatType;
  }

  private static ChatTypeDecoration readDecoration(NbtMap decorationData) {
    var translationKey = decorationData.getString("translation_key");
    var parameters = decorationData.getList("parameters", NbtType.STRING)
      .stream()
      .map(ChatTypeDecoration.Parameter::valueOf)
      .toList();
    var style = decorationData.getCompound("style");

    return new ChatType.ChatTypeDecorationImpl(translationKey, parameters, style);
  }

  @SneakyThrows
  private static Style deserializeStyle(NbtMap styleData) {
    var output = new ByteArrayOutputStream();
    new NBTOutputStream(new DataOutputStream(output)).writeValue(styleData, 512);
    var lenniStyleNbt = NBT_STYLE_SERIALIZER.deserialize(NbtIO.JAVA.getReader().readCompound(new DataInputStream(new ByteArrayInputStream(output.toByteArray())), NbtReadTracker.unlimited()));
    var lenniStyleJson = JSON_STYLE_SERIALIZER.serialize(lenniStyleNbt);
    return GsonComponentSerializer.gson().serializer().fromJson(lenniStyleJson, Style.class);
  }

  public TranslatableComponent buildChatComponent(BoundChatMessageInfo chatInfo) {
    return buildComponent(mcplChatType.chat(), chatInfo);
  }

  public TranslatableComponent buildComponent(ChatTypeDecoration decoration, BoundChatMessageInfo chatInfo) {
    var translationArgs = new ArrayList<ComponentLike>();
    for (var parameter : decoration.parameters()) {
      switch (parameter) {
        case ChatTypeDecoration.Parameter.CONTENT -> translationArgs.add(chatInfo.content);
        case ChatTypeDecoration.Parameter.SENDER -> translationArgs.add(chatInfo.sender);
        case ChatTypeDecoration.Parameter.TARGET -> translationArgs.add(Objects.requireNonNullElse(chatInfo.target, Component.empty()));
      }
    }

    return Component.translatable(decoration.translationKey(), null, deserializeStyle(decoration.style()), translationArgs);
  }

  public record BoundChatMessageInfo(Component content, Component sender, @Nullable Component target) {
  }
}
