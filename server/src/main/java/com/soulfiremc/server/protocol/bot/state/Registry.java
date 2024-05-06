package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.ResourceKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class Registry<T> {
  private final Object2ObjectMap<ResourceKey, T> byKey = new Object2ObjectOpenHashMap<>();
  private final Int2ObjectMap<T> byId = new Int2ObjectOpenHashMap<>();

  public void register(ResourceKey key, int id, T value) {
    byKey.put(key, value);
    byId.put(id, value);
  }

  public T get(ResourceKey key) {
    return byKey.get(key);
  }

  public T get(int id) {
    return byId.get(id);
  }

  public int size() {
    return byKey.size();
  }

  public ResourceKey getKey(T value) {
    for (var entry : byKey.object2ObjectEntrySet()) {
      if (entry.getValue() == value) {
        return entry.getKey();
      }
    }

    throw new IllegalArgumentException("Value not found in registry");
  }

  public int getId(T value) {
    for (var entry : byId.int2ObjectEntrySet()) {
      if (entry.getValue() == value) {
        return entry.getIntKey();
      }
    }

    throw new IllegalArgumentException("Value not found in registry");
  }
}
