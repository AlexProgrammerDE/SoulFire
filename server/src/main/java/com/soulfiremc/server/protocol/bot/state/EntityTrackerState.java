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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityTrackerState {
  private final Int2ObjectMap<Entity> entitiesMap = new Int2ObjectOpenHashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public void addEntity(Entity entity) {
    try {
      lock.writeLock().lock();
      entitiesMap.put(entity.entityId(), entity);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void removeEntity(int entityId) {
    try {
      lock.writeLock().lock();
      entitiesMap.remove(entityId);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public @Nullable Entity getEntity(int entityId) {
    try {
      lock.readLock().lock();
      return entitiesMap.get(entityId);
    } finally {
      lock.readLock().unlock();
    }
  }

  public Collection<Entity> getEntities() {
    try {
      lock.readLock().lock();
      return List.copyOf(entitiesMap.values());
    } finally {
      lock.readLock().unlock();
    }
  }

  public void tick() {
    getEntities().forEach(entity -> {
      entity.setOldPosAndRot();
      entity.tick();
    });
  }
}
