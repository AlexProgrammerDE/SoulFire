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
package com.soulfiremc.server.settings.instance;

import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class AccountSettings implements SettingsObject {
  private static final String NAMESPACE = "account";
  public static final StringProperty<InstanceSettingsSource> NAME_FORMAT =
    ImmutableStringProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("name-format")
      .uiName("Name format")
      .description("The format of the bot names. %d will be replaced with the bot number.")
      .defaultValue("Bot_%d")
      .build();
  public static final BooleanProperty<InstanceSettingsSource> SHUFFLE_ACCOUNTS =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("shuffle-accounts")
      .uiName("Shuffle accounts")
      .description("Should the accounts order be random when connecting bots?")
      .defaultValue(false)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> USE_PROXIES_FOR_ACCOUNT_AUTH =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("use-proxies-for-account-auth")
      .uiName("Use proxies for account auth")
      .description("""
        Should the imported proxies be used to authenticate accounts? (Contact Microsoft login, input credentials, etc.)
        Otherwise the SF server will authenticate accounts directly.""")
      .defaultValue(false)
      .build();
  public static final IntProperty<InstanceSettingsSource> ACCOUNT_IMPORT_CONCURRENCY =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("account-import-concurrency")
      .uiName("Account import concurrency")
      .description("For credentials-like auth, how many accounts should be imported at once?")
      .defaultValue(3)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
}
