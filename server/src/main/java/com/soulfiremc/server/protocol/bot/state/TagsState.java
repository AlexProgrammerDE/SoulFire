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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.RegistryValue;
import com.soulfiremc.server.data.TagKey;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.kyori.adventure.key.Key;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
public class TagsState {
  private static final int[] EMPTY_INT_ARRAY = new int[0];
  private final Map<Key, Map<Key, int[]>> tags = new Object2ObjectOpenHashMap<>();

  public void handleTagData(Map<Key, Map<Key, int[]>> updateTags) {
    tags.putAll(updateTags);
  }

  public <T extends RegistryValue<T>> boolean is(T value, TagKey<T> tagKey) {
    return Arrays.stream(getValuesOfTag(tagKey)).anyMatch(t -> t == value.id());
  }

  public <T extends RegistryValue<T>> List<TagKey<T>> getTags(T value) {
    return tags.getOrDefault(value.registry().registryKey().key(), Map.of()).entrySet().stream()
      .filter(entry -> Arrays.stream(entry.getValue()).anyMatch(t -> t == value.id()))
      .map(entry -> new TagKey<>(value.registry().registryKey(), entry.getKey()))
      .toList();
  }

  public <T extends RegistryValue<T>> int[] getValuesOfTag(TagKey<T> tagKey) {
    return tags.getOrDefault(tagKey.registry().key(), Map.of())
      .getOrDefault(tagKey.key(), EMPTY_INT_ARRAY);
  }

  public Map<Key, Map<Key, int[]>> exportTags() {
    return Map.copyOf(tags);
  }
}
