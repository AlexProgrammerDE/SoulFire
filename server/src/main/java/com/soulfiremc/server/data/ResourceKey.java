package com.soulfiremc.server.data;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

public record ResourceKey<T extends RegistryValue<T>>(Key registryKey, Key key) {
  public static final Key ROOT_REGISTRY_KEY = Key.key("minecraft:root");

  public static <T extends RegistryValue<T>> ResourceKey<T> key(@KeyPattern String key) {
    return new ResourceKey<>(ROOT_REGISTRY_KEY, Key.key(key));
  }
}
