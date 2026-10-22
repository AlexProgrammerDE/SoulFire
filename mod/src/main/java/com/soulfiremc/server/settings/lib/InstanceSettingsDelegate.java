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
package com.soulfiremc.server.settings.lib;

import com.google.gson.JsonElement;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.property.Property;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class InstanceSettingsDelegate implements InstanceSettingsSource {
  private final Supplier<InstanceSettingsSource> loader;
  private volatile InstanceSettingsSource source;

  public InstanceSettingsDelegate(Supplier<InstanceSettingsSource> loader) {
    this.loader = loader;
    invalidate();
  }

  /// Reloads after a committed settings write. Readers keep the previous snapshot
  /// until loading succeeds and never acquire a database connection or this lock.
  public synchronized void invalidate() {
    source = Objects.requireNonNull(loader.get());
  }

  @Override
  public Map<UUID, MinecraftAccount> accounts() {
    return source.accounts();
  }

  @Override
  public List<SFProxy> proxies() {
    return source.proxies();
  }

  @Override
  public ServerSettingsSource serverSettings() {
    return source.serverSettings();
  }

  @Override
  public InstanceSettingsImpl.Stem stem() {
    return source.stem();
  }

  @Override
  public Optional<JsonElement> get(Property<SettingsSource.Instance> property) {
    return source.get(property);
  }
}
