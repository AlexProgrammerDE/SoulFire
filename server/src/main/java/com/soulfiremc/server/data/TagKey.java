package com.soulfiremc.server.data;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

public record TagKey<T extends RegistryValue>(Key key) {
  public static <T extends RegistryValue> TagKey<T> key(@KeyPattern String  key) {
    return new TagKey<>(Key.key(key));
  }
}
