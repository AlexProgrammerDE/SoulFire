/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.state.tag;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.data.EntityType;
import net.pistonmaster.serverwrecker.data.ItemType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TagsState {
    private final Map<String, Set<BlockType>> blockTags = new Object2ObjectOpenHashMap<>();
    private final Map<String, Set<ItemType>> itemTags = new Object2ObjectOpenHashMap<>();
    private final Map<String, Set<EntityType>> entityTags = new Object2ObjectOpenHashMap<>();

    private static String stripMinecraft(String input) {
        return input.replace("minecraft:", "");
    }

    public void handleTagData(Map<String, Map<String, int[]>> tags) {
        for (Map.Entry<String, Map<String, int[]>> registry : tags.entrySet()) {
            String registryKey = stripMinecraft(registry.getKey());

            switch (registryKey) {
                case "block" -> handleBlocks(registry.getValue());
                case "item" -> handleItems(registry.getValue());
                case "entity_type" -> handleEntities(registry.getValue());
                // Ignore everything else, we just need these three for now
            }
        }
    }

    private void handleBlocks(Map<String, int[]> blocks) {
        for (Map.Entry<String, int[]> block : blocks.entrySet()) {
            String blockKey = stripMinecraft(block.getKey());
            Set<BlockType> blockSet = blockTags.computeIfAbsent(blockKey, k -> new HashSet<>(block.getValue().length));

            for (int i : block.getValue()) {
                blockSet.add(BlockType.getById(i));
            }
        }
    }

    private void handleItems(Map<String, int[]> items) {
        for (Map.Entry<String, int[]> item : items.entrySet()) {
            String itemKey = stripMinecraft(item.getKey());
            Set<ItemType> itemSet = itemTags.computeIfAbsent(itemKey, k -> new HashSet<>(item.getValue().length));

            for (int i : item.getValue()) {
                itemSet.add(ItemType.getById(i));
            }
        }
    }

    private void handleEntities(Map<String, int[]> entities) {
        for (Map.Entry<String, int[]> entity : entities.entrySet()) {
            String entityKey = stripMinecraft(entity.getKey());
            Set<EntityType> entitySet = entityTags.computeIfAbsent(entityKey, k -> new HashSet<>(entity.getValue().length));

            for (int i : entity.getValue()) {
                entitySet.add(EntityType.getById(i));
            }
        }
    }
}
