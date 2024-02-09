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

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.AccessLevel;
import lombok.With;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record AttributeType(String name, double min, double max, double defaultValue) {
    public static final Object2ReferenceMap<String, AttributeType> FROM_NAME = new Object2ReferenceOpenHashMap<>();

    // VALUES REPLACE

    public static AttributeType register(String name) {
        var attributeType = GsonDataHelper.fromJson("/minecraft/attributes.json", name, AttributeType.class);

        FROM_NAME.put(attributeType.name(), attributeType);
        return attributeType;
    }

    public static AttributeType getByName(String name) {
        return FROM_NAME.get(name.replace("minecraft:", ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeType attributeType)) return false;
        return name.equals(attributeType.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
