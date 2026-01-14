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
package com.soulfiremc.server.bot;

import com.google.protobuf.Value;
import com.soulfiremc.grpc.generated.BotMetadata;
import com.soulfiremc.grpc.generated.BotProto;
import lombok.With;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent bot configuration stored in the instance profile.
 * This is separate from BotConnection which is the runtime state.
 */
@With
public record BotEntity(
  @NonNull UUID id,
  @NonNull UUID accountId,
  @Nullable UUID proxyId,
  @NonNull Map<String, Value> metadata,
  boolean enabled,
  @Nullable String displayName
) {
  public BotEntity {
    // Ensure metadata is mutable for updates
    metadata = new ConcurrentHashMap<>(metadata);
  }

  public static BotEntity create(UUID accountId, @Nullable UUID proxyId) {
    return new BotEntity(
      UUID.randomUUID(),
      accountId,
      proxyId,
      new ConcurrentHashMap<>(),
      true,
      null
    );
  }

  public static BotEntity fromProto(BotProto proto) {
    return new BotEntity(
      UUID.fromString(proto.getId()),
      UUID.fromString(proto.getAccountId()),
      proto.hasProxyId() ? UUID.fromString(proto.getProxyId()) : null,
      new ConcurrentHashMap<>(proto.getMetadata().getEntriesMap()),
      proto.getEnabled(),
      proto.hasDisplayName() ? proto.getDisplayName() : null
    );
  }

  public BotProto toProto() {
    var builder = BotProto.newBuilder()
      .setId(id.toString())
      .setAccountId(accountId.toString())
      .setMetadata(BotMetadata.newBuilder().putAllEntries(metadata).build())
      .setEnabled(enabled);

    if (proxyId != null) {
      builder.setProxyId(proxyId.toString());
    }
    if (displayName != null) {
      builder.setDisplayName(displayName);
    }

    return builder.build();
  }

  // Metadata helpers
  public void setMetadata(String key, Value value) {
    metadata.put(key, value);
  }

  public Value getMetadata(String key) {
    return metadata.get(key);
  }

  public void removeMetadata(String key) {
    metadata.remove(key);
  }

  public void clearMetadata() {
    metadata.clear();
  }

  public void mergeMetadata(Map<String, Value> updates) {
    metadata.putAll(updates);
  }
}
