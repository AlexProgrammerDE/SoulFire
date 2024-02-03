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
package net.pistonmaster.soulfire.server.protocol.bot.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.Entity;
import org.jetbrains.annotations.Nullable;

@Data
public class EntityTrackerState {
    private final Int2ObjectMap<Entity> entities = new Int2ObjectOpenHashMap<>();

    public void addEntity(Entity entity) {
        entities.put(entity.entityId(), entity);
    }

    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }

    public @Nullable Entity getEntity(int entityId) {
        return entities.get(entityId);
    }

    public void tick() {
        entities.values().forEach(Entity::tick);
    }
}
