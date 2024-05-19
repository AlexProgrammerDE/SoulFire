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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.key.Key;

@Getter
public class TagsState {
  private final Map<Key, Map<Key, IntSet>> tags = new Object2ObjectOpenHashMap<>();

  @SuppressWarnings("PatternValidation")
  public void handleTagData(Map<String, Map<String, int[]>> updateTags) {
    for (var entry : updateTags.entrySet()) {
      var tagMap = new Object2ObjectOpenHashMap<Key, IntSet>();
      for (var tagEntry : entry.getValue().entrySet()) {
        var set = new IntOpenHashSet(tagEntry.getValue());
        tagMap.put(Key.key(tagEntry.getKey()), set);
      }
      tags.put(Key.key(entry.getKey()), tagMap);
    }
  }

  public <T extends RegistryValue<T>> boolean isValueInTag(T value, TagKey<T> tagKey) {
    return getValuesOfTag(value, tagKey).contains(value.id());
  }

  public <T extends RegistryValue<T>> IntSet getValuesOfTag(T value, TagKey<T> tagKey) {
    return tags.getOrDefault(tagKey.registry().key(), Map.of())
      .getOrDefault(tagKey.key(), IntSet.of());
  }

  public Map<String, Map<String, int[]>> exportTags() {
    var result = new Object2ObjectOpenHashMap<String, Map<String, int[]>>();
    for (var entry : tags.entrySet()) {
      var tagMap = new Object2ObjectOpenHashMap<String, int[]>();
      for (var tagEntry : entry.getValue().entrySet()) {
        tagMap.put(tagEntry.getKey().toString(), tagEntry.getValue().toIntArray());
      }
      result.put(entry.getKey().toString(), tagMap);
    }
    return result;
  }
}
