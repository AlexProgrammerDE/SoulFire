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
package net.pistonmaster.serverwrecker.server.protocol.bot.state.tag;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.data.EntityType;
import net.pistonmaster.serverwrecker.server.data.ItemType;

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
        for (var registry : tags.entrySet()) {
            var registryKey = stripMinecraft(registry.getKey());

            switch (registryKey) {
                case "block" -> handleBlocks(registry.getValue());
                case "item" -> handleItems(registry.getValue());
                case "entity_type" -> handleEntities(registry.getValue());
                // Ignore everything else, we just need these three for now
            }
        }
    }

    private void handleBlocks(Map<String, int[]> blocks) {
        for (var block : blocks.entrySet()) {
            var blockKey = stripMinecraft(block.getKey());
            var blockSet = blockTags.computeIfAbsent(blockKey, k -> new HashSet<>(block.getValue().length));

            for (var i : block.getValue()) {
                blockSet.add(BlockType.getById(i));
            }
        }
    }

    private void handleItems(Map<String, int[]> items) {
        for (var item : items.entrySet()) {
            var itemKey = stripMinecraft(item.getKey());
            var itemSet = itemTags.computeIfAbsent(itemKey, k -> new HashSet<>(item.getValue().length));

            for (var i : item.getValue()) {
                itemSet.add(ItemType.getById(i));
            }
        }
    }

    private void handleEntities(Map<String, int[]> entities) {
        for (var entity : entities.entrySet()) {
            var entityKey = stripMinecraft(entity.getKey());
            var entitySet = entityTags.computeIfAbsent(entityKey, k -> new HashSet<>(entity.getValue().length));

            for (var i : entity.getValue()) {
                entitySet.add(EntityType.getById(i));
            }
        }
    }

    public boolean isBlockInTag(BlockType blockType, String tagName) {
        return blockTags.getOrDefault(tagName, Set.of()).contains(blockType);
    }

    public boolean isItemInTag(ItemType itemType, String tagName) {
        return itemTags.getOrDefault(tagName, Set.of()).contains(itemType);
    }

    public boolean isEntityInTag(EntityType entityType, String tagName) {
        return entityTags.getOrDefault(tagName, Set.of()).contains(entityType);
    }
}
