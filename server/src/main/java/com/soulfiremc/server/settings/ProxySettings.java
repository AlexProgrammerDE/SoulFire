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
package com.soulfiremc.server.settings;

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.util.BuiltinSettingsConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySettings implements SettingsObject {
  private static final Property.Builder BUILDER =
    Property.builder(BuiltinSettingsConstants.PROXY_SETTINGS_ID);
  public static final IntProperty BOTS_PER_PROXY =
    BUILDER.ofInt(
      "bots-per-proxy",
      "Bots per proxy",
      new String[] {"--bots-per-proxy"},
      "Amount of bots that can be on a single proxy",
      -1,
      -1,
      Integer.MAX_VALUE,
      1);
  public static final BooleanProperty SHUFFLE_PROXIES =
    BUILDER.ofBoolean(
      "shuffle-proxies",
      "Shuffle proxies",
      new String[] {"--shuffle-proxies"},
      "Should the proxy order be random when connecting bots?",
      false);
}
