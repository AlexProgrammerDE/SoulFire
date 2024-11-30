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

import com.google.gson.JsonElement;
import com.soulfiremc.server.data.Registry;
import com.soulfiremc.server.data.RegistryValue;
import com.soulfiremc.server.util.SFHelpers;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.lenni0451.mcstructs.nbt.INbtTag;
import net.lenni0451.mcstructs.nbt.io.NbtIO;
import net.lenni0451.mcstructs.nbt.io.NbtReadTracker;
import net.lenni0451.mcstructs.text.serializer.TextComponentCodec;
import net.lenni0451.mcstructs.text.serializer.subtypes.IStyleSerializer;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

@Getter
public class SFChatType implements RegistryValue<SFChatType> {
  private static final TextComponentCodec CODEC = TextComponentCodec.LATEST;
  private static final IStyleSerializer<INbtTag> NBT_STYLE_SERIALIZER = CODEC.getNbtSerializer().getStyleSerializer();
  private static final IStyleSerializer<JsonElement> JSON_STYLE_SERIALIZER = CODEC.getJsonSerializer().getStyleSerializer();
  private final Key key;
  private final int id;
  private final Registry<SFChatType> registry;
  private final ChatType mcplChatType;

  public SFChatType(Key key, int id, Registry<SFChatType> registry, NbtMap chatTypeData) {
    this.key = key;
    this.id = id;
    this.registry = registry;
    this.mcplChatType = new ChatType(
      readDecoration(chatTypeData.getCompound("chat")),
      readDecoration(chatTypeData.getCompound("narration"))
    );
  }

  private static ChatTypeDecoration readDecoration(NbtMap decorationData) {
    return new ChatType.ChatTypeDecorationImpl(
      decorationData.getString("translation_key"),
      decorationData.getList("parameters", NbtType.STRING)
        .stream()
        .map(s -> s.toUpperCase(Locale.ROOT))
        .map(ChatTypeDecoration.Parameter::valueOf)
        .toList(),
      decorationData.getCompound("style", null)
    );
  }

  @SneakyThrows
  private static Style deserializeStyle(NbtMap styleData) {
    if (styleData == null) {
      return Style.empty();
    }

    var output = new ByteArrayOutputStream();
    new NBTOutputStream(new DataOutputStream(output)).writeValue(styleData, 512);
    var lenniStyleNbt = NBT_STYLE_SERIALIZER.deserialize(NbtIO.JAVA.getReader()
      .readCompound(new DataInputStream(new ByteArrayInputStream(output.toByteArray())), NbtReadTracker.unlimited()));
    var lenniStyleJson = JSON_STYLE_SERIALIZER.serialize(lenniStyleNbt);
    return GsonComponentSerializer.gson().serializer().fromJson(lenniStyleJson, Style.class);
  }

  public static TranslatableComponent buildChatComponent(ChatType mcplChatType, BoundChatMessageInfo chatInfo) {
    return buildComponent(mcplChatType.chat(), chatInfo);
  }

  public static TranslatableComponent buildComponent(ChatTypeDecoration decoration, BoundChatMessageInfo chatInfo) {
    var translationArgs = new ArrayList<ComponentLike>();
    for (var parameter : decoration.parameters()) {
      SFHelpers.mustSupply(() -> switch (parameter) {
        case ChatTypeDecoration.Parameter.CONTENT -> () -> translationArgs.add(chatInfo.content);
        case ChatTypeDecoration.Parameter.SENDER -> () -> translationArgs.add(chatInfo.sender);
        case ChatTypeDecoration.Parameter.TARGET -> () -> translationArgs.add(Objects.requireNonNullElse(chatInfo.target, Component.empty()));
      });
    }

    return Component.translatable(decoration.translationKey(), null, deserializeStyle(decoration.style()), translationArgs);
  }

  public record BoundChatMessageInfo(Component content, Component sender, @Nullable Component target) {
  }
}
