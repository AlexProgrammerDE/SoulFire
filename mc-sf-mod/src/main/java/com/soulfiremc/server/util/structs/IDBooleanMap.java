package com.soulfiremc.server.util.structs;

import net.minecraft.core.Registry;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class IDBooleanMap<K> {
  private final ToIntFunction<K> idFunction;
  private final boolean[] values;

  public IDBooleanMap(Registry<K> registry, Predicate<K> valueFunction) {
    this(registry.stream().toList(), registry::getId, valueFunction);
  }

  public IDBooleanMap(Collection<K> collection, ToIntFunction<K> idFunction, Predicate<K> valueFunction) {
    this.idFunction = idFunction;
    values = new boolean[collection.size()];

    for (var key : collection) {
      this.values[idFunction.applyAsInt(key)] = valueFunction.test(key);
    }
  }

  public boolean get(K key) {
    return values[idFunction.applyAsInt(key)];
  }
}
