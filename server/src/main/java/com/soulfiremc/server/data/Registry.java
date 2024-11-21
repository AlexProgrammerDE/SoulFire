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
package com.soulfiremc.server.data;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;

import java.util.Collection;

public class Registry<T extends RegistryValue<T>> {
  @Getter
  private final ResourceKey<? extends Registry<T>> registryKey;
  private final Object2ReferenceMap<Key, T> FROM_KEY = new Object2ReferenceOpenHashMap<>();
  private final Int2ReferenceMap<T> FROM_ID = new Int2ReferenceOpenHashMap<>();

  @SuppressWarnings("unchecked")
  public Registry(ResourceKey<?> registryKey) {
    this.registryKey = (ResourceKey<? extends Registry<T>>) registryKey;
  }

  public T register(final T value) {
    FROM_KEY.put(value.key(), value);
    FROM_ID.put(value.id(), value);

    return value;
  }

  public T getById(int id) {
    return FROM_ID.get(id);
  }

  public T getByKey(Key key) {
    return FROM_KEY.get(key);
  }

  public Collection<T> values() {
    return FROM_KEY.values();
  }

  public int size() {
    return FROM_KEY.size();
  }

  public RegistryDataWriter writer(FromRegistryDataFactory<T> factory) {
    return (key, id, data) -> register(factory.create(key, id, this, data));
  }

  public interface RegistryDataWriter {
    RegistryDataWriter NO_OP = (key, id, data) -> {};

    void register(Key key, int id, NbtMap data);
  }

  public interface FromRegistryDataFactory<T extends RegistryValue<T>> {
    T create(Key key, int id, Registry<T> registry, NbtMap data);
  }
}
