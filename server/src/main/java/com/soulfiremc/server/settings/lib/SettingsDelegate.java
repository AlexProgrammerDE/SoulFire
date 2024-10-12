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
package com.soulfiremc.server.settings.lib;

import com.google.gson.JsonElement;
import com.soulfiremc.server.settings.PropertyKey;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.proxy.SFProxy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
public class SettingsDelegate implements SettingsSource {
  private SettingsImpl source;

  @Override
  public List<MinecraftAccount> accounts() {
    return source.accounts();
  }

  @Override
  public List<SFProxy> proxies() {
    return source.proxies();
  }

  @Override
  public Optional<JsonElement> get(PropertyKey key) {
    return source.get(key);
  }
}
