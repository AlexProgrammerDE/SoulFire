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
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.util.BuiltinSettingsConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class AccountSettings implements SettingsObject {
  private static final Property.Builder BUILDER =
    Property.builder(BuiltinSettingsConstants.ACCOUNT_SETTINGS_ID);
  public static final StringProperty NAME_FORMAT =
    BUILDER.ofString(
      "name-format",
      "Name format",
      new String[] {"--name-format"},
      "The format of the bot names. %d will be replaced with the bot number.",
      "Bot_%d");
  public static final BooleanProperty SHUFFLE_ACCOUNTS =
    BUILDER.ofBoolean(
      "shuffle-accounts",
      "Shuffle accounts",
      new String[] {"--shuffle-accounts"},
      "Should the accounts order be random when connecting bots?",
      false);
}
