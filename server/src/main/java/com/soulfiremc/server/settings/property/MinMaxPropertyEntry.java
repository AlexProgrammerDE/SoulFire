package com.soulfiremc.server.settings.property;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class MinMaxPropertyEntry {
  public abstract String uiName();

  public abstract String description();

  public abstract int defaultValue();

  @Value.Default
  public String placeholder() {
    return "";
  }
}
