package com.soulfiremc.server.util;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Make an object use shared memory.
 * Will try to free memory when the object is not used/already in memory.
 *
 * @param <T> The type of object to pool.
 */
public class ReferenceCache<T> {
  private final Map<T, T> cache = new WeakHashMap<>();

  public T poolReference(T obj) {
    synchronized (cache) {
      return cache.computeIfAbsent(obj, Function.identity());
    }
  }
}
