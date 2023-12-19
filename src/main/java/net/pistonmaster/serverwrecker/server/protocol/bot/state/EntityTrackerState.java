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
package net.pistonmaster.serverwrecker.server.protocol.bot.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.entity.Entity;

import java.util.Map;

@Data
public class EntityTrackerState {
    private final Map<Integer, Entity> entities = new Int2ObjectOpenHashMap<>();

    public void addEntity(Entity entity) {
        entities.put(entity.entityId(), entity);
    }

    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }

    public Entity getEntity(int entityId) {
        return entities.get(entityId);
    }

    public void tick() {
        entities.values().forEach(Entity::tick);
    }
}
