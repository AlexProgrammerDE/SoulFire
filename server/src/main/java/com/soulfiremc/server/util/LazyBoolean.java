package com.soulfiremc.server.util;

import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LazyBoolean {
  private final BooleanSupplier supplier;
  private boolean value;
  private boolean initialized;

  public boolean get() {
    if (!initialized) {
      value = supplier.getAsBoolean();
      initialized = true;
    }

    return value;
  }
}
