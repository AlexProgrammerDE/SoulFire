package com.soulfiremc.server.util.structs;

import net.minecraft.core.Registry;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class IDMap<K, V> {
  private final ToIntFunction<K> idFunction;
  private final Object[] values;

  public IDMap(Registry<K> registry, Function<K, V> valueFunction) {
    this(registry.stream().toList(), registry::getId, valueFunction);
  }

  public IDMap(Collection<K> collection, ToIntFunction<K> idFunction, Function<K, V> valueFunction) {
    this.idFunction = idFunction;
    this.values = new Object[collection.size()];

    for (var key : collection) {
      this.values[idFunction.applyAsInt(key)] = valueFunction.apply(key);
    }
  }

  @SuppressWarnings("unchecked")
  public V get(K key) {
    return (V) this.values[idFunction.applyAsInt(key)];
  }
}
