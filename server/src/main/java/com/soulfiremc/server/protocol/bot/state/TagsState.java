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

import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.FluidType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.data.RegistryKeys;
import com.soulfiremc.server.data.ResourceKey;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class TagsState {
  private final Map<ResourceKey, Map<ResourceKey, IntSet>> tags = new Object2ObjectOpenHashMap<>();

  public void handleTagData(Map<String, Map<String, int[]>> updateTags) {
    for (var entry : updateTags.entrySet()) {
      var tagMap = new Object2ObjectOpenHashMap<ResourceKey, IntSet>();
      for (var tagEntry : entry.getValue().entrySet()) {
        var set = new IntOpenHashSet(tagEntry.getValue());
        tagMap.put(ResourceKey.fromString(tagEntry.getKey()), set);
      }
      tags.put(ResourceKey.fromString(entry.getKey()), tagMap);
    }
  }

  public boolean isBlockInTag(BlockType blockType, ResourceKey tagKey) {
    return tags.getOrDefault(RegistryKeys.BLOCK, Map.of())
      .getOrDefault(tagKey, IntSet.of())
      .contains(blockType.id());
  }

  public boolean isItemInTag(ItemType itemType, ResourceKey tagKey) {
    return tags.getOrDefault(RegistryKeys.ITEM, Map.of())
      .getOrDefault(tagKey, IntSet.of())
      .contains(itemType.id());
  }

  public boolean isEntityInTag(EntityType entityType, ResourceKey tagKey) {
    return tags.getOrDefault(RegistryKeys.ENTITY_TYPE, Map.of())
      .getOrDefault(tagKey, IntSet.of())
      .contains(entityType.id());
  }

  public boolean isFluidInTag(FluidType fluidType, ResourceKey tagKey) {
    return tags.getOrDefault(RegistryKeys.FLUID, Map.of())
      .getOrDefault(tagKey, IntSet.of())
      .contains(fluidType.id());
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
