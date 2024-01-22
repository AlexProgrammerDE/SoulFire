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
package net.pistonmaster.soulfire.data;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

@SuppressWarnings("unused")
public record EntityType(int id, String name, float width, float height,
                         String category, boolean friendly) {
    public static final Int2ReferenceMap<EntityType> FROM_ID = new Int2ReferenceOpenHashMap<>();

    // VALUES REPLACE

    public static EntityType register(String name) {
        var entityType = GsonDataHelper.fromJson("/minecraft/entities.json", name, EntityType.class);
        FROM_ID.put(entityType.id(), entityType);
        return entityType;
    }

    public static EntityType getById(int id) {
        return FROM_ID.get(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityType entityType)) return false;
        return id == entityType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
