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

import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class EntityTrackerState {
  private final Int2ObjectMap<Entity> entitiesMap = new Int2ObjectOpenHashMap<>();

  public void addEntity(Entity entity) {
    entitiesMap.put(entity.entityId(), entity);
  }

  public void removeEntity(int entityId) {
    entitiesMap.remove(entityId);
  }

  public @Nullable Entity getEntity(int entityId) {
    return entitiesMap.get(entityId);
  }

  public Collection<Entity> getEntities() {
    return entitiesMap.values();
  }

  public void tick() {
    entitiesMap.values().forEach(Entity::tick);
  }
}
