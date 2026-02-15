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
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public final class BotSettingsDelegate implements BotSettingsSource {
  private final CachedLazyObject<BotSettingsSource> source;

  public void invalidate() {
    source.invalidate();
  }

  @Override
  public Optional<JsonElement> get(Property<SettingsSource.Bot> property) {
    return source.get().get(property);
  }

  @Override
  public InstanceSettingsSource instanceSettings() {
    return source.get().instanceSettings();
  }

  @Override
  public MinecraftAccount stem() {
    return source.get().stem();
  }
}
