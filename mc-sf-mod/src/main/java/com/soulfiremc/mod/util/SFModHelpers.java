package com.soulfiremc.mod.util;

import net.lenni0451.reflect.Objects;
import net.lenni0451.reflect.stream.RStream;

public class SFModHelpers {
  @SuppressWarnings("unchecked")
  public static <T> T deepCopy(T object) {
    var newObject = (T) Objects.allocate(object.getClass());
    RStream.of(object)
      .withSuper()
      .fields()
      .filter(false)
      .forEach(field -> field.copy(newObject));
    return newObject;
  }
}
