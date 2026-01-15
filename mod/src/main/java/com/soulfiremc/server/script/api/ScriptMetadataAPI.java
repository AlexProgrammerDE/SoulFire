/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.api.metadata.MetadataKey;
import net.kyori.adventure.key.Key;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.intellij.lang.annotations.Subst;

import java.util.function.Supplier;

public record ScriptMetadataAPI(MetadataHolder metadata) {
  @HostAccess.Export
  public Value getOrSet(String namespace, String key, Supplier<Value> defaultValue) {
    return this.metadata.getOrSet(getMetaKey(namespace, key), defaultValue);
  }

  @HostAccess.Export
  public Value getOrDefault(String namespace, String key, Value defaultValue) {
    return this.metadata.getOrDefault(getMetaKey(namespace, key), defaultValue);
  }

  @HostAccess.Export
  public Value get(String namespace, String key) {
    return this.metadata.get(getMetaKey(namespace, key));
  }

  @HostAccess.Export
  public void set(String namespace, String key, Value value) {
    this.metadata.set(getMetaKey(namespace, key), value);
  }

  @HostAccess.Export
  public void remove(String namespace, String key) {
    this.metadata.remove(getMetaKey(namespace, key));
  }

  @HostAccess.Export
  public Value getAndRemove(String namespace, String key) {
    return this.metadata.getAndRemove(getMetaKey(namespace, key));
  }

  private MetadataKey<Value> getMetaKey(@Subst("plugin") String namespace, @Subst("key") String key) {
    return new MetadataKey<>(Key.key("soulfire_plugin_" + namespace, key), Value.class);
  }
}
