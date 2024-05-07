package com.soulfiremc.server.protocol.bot.container;

import java.util.Map;
import java.util.Optional;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

public record SFDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents) {
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getOptional(DataComponentType<T> type) {
    // DataComponents can be a null values in this HashMap (even if containsKey() == true)
    // This means it will even remove the explicit vanilla default value for the component key
    var component = dataComponents.get(type);
    return component == null ? Optional.empty() : Optional.of((T) component.getValue());
  }

  public <T> T get(DataComponentType<T> type) {
    return getOptional(type).orElseThrow();
  }
}
