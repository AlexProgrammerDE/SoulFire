/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.NamedEntityData;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;

import java.util.*;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class EntityMetadataState {
  private final Entity entity;
  private final Int2ObjectMap<EntityMetadata<?, ?>> metadataStore = new Int2ObjectOpenHashMap<>();

  private NamedEntityData fromMetadata(EntityMetadata<?, ?> metadata) {
    return NamedEntityData.VALUES.stream()
      .filter(namedData -> entity.entityType().inheritedClasses().contains(namedData.entityClass()))
      .filter(namedData -> namedData.networkId() == metadata.getId())
      .findFirst()
      .orElseThrow();
  }

  public void set(EntityMetadata<?, ?> metadata) {
    set(fromMetadata(metadata), metadata);
  }

  private void set(NamedEntityData namedEntityData, EntityMetadata<?, ?> metadata) {
    var existing = this.metadataStore.get(metadata.getId());
    if (!Objects.equals(existing.getValue(), metadata.getValue())) {
      this.metadataStore.put(metadata.getId(), metadata);
      this.entity.onSyncedDataUpdated(namedEntityData);
    }
  }

  public <V, T extends MetadataType<V>> void set(NamedEntityData namedEntityData, T metadataType, MetadataFactory<V, T> factory, V value) {
    set(namedEntityData, factory.create(namedEntityData.networkId(), metadataType, value));
  }

  public <T> T get(NamedEntityData namedEntityData, MetadataType<T> metadataType) {
    return get(namedEntityData.networkId(), metadataType).orElseThrow(() -> new IllegalArgumentException("Metadata not found"));
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(int id, MetadataType<T> metadataType) {
    assert metadataType != null;

    var metadata = this.metadataStore.get(id);
    if (metadata == null) {
      return Optional.empty();
    }

    return Optional.of((T) metadata.getValue());
  }

  public Map<String, ?> toNamedMap() {
    var namedMap = new LinkedHashMap<String, Object>();
    entity.entityType().inheritedClasses().stream().flatMap(clazz -> {
        var stream = Stream.<NamedEntityData>empty();
        for (var namedData : NamedEntityData.VALUES) {
          if (namedData.entityClass().equals(clazz)) {
            stream = Stream.concat(stream, Stream.of(namedData));
          }
        }

        return stream;
      })
      .forEach(namedData -> {
        var metadata = this.metadataStore.get(namedData.networkId());
        if (metadata != null) {
          namedMap.put(namedData.key(), metadata.getValue());
        }
      });

    return namedMap;
  }

  public void assignValues(Collection<EntityMetadata<?, ?>> other) {
    for (var metadata : other) {
      this.metadataStore.put(metadata.getId(), metadata);
      this.entity.onSyncedDataUpdated(fromMetadata(metadata));
    }
  }

  public interface MetadataFactory<V, T extends MetadataType<V>> {
    EntityMetadata<V, ? extends MetadataType<V>> create(int id, T type, V value);
  }
}
