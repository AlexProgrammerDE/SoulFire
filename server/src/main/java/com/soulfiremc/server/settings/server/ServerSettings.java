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
package com.soulfiremc.server.settings.server;

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerSettings implements SettingsObject {
  private static final String NAMESPACE = "server";
  public static final StringProperty PUBLIC_ADDRESS =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("public-address")
      .uiName("Public address")
      .description("The address clients on the internet use to connect to this SoulFire instance.\nUsed for links in E-Mails.")
      .defaultValue("http://127.0.0.1:38765")
      .build();
  public static final BooleanProperty HAVE_I_BEEN_PWNED_CHECK =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("have-i-been-pwned-check")
      .uiName("Have I Been Pwned check")
      .description("Check if passwords are strong using the secure haveibeenpwned.com API")
      .defaultValue(true)
      .build();
  public static final IntProperty HAVE_I_BEEN_PWNED_LIMIT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("have-i-been-pwned-limit")
      .uiName("Have I Been Pwned limit")
      .description("If the password is used more than this number of times, it is considered unsafe.")
      .defaultValue(0)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .stepValue(1)
      .build();
  public static final BooleanProperty ALLOW_REGISTRATION =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("allow-registration")
      .uiName("Allow registration")
      .description("Allow anyone to register an account.")
      .defaultValue(false)
      .build();
  public static final BooleanProperty ALLOW_CREATING_INSTANCES =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("allow-creating-instances")
      .uiName("Allow creating instances")
      .description("Allow (non-admin) users to create instances.")
      .defaultValue(true)
      .build();
  public static final BooleanProperty ALLOW_DELETING_INSTANCES =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("allow-deleting-instances")
      .uiName("Allow deleting instances")
      .description("Allow the owner of an instance to delete it.")
      .defaultValue(true)
      .build();
  public static final BooleanProperty ALLOW_CHANGING_INSTANCE_META =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("allow-changing-instance-meta")
      .uiName("Allow changing instance meta")
      .description("Allow the owner of an instance to change meta like instance name and icon.")
      .defaultValue(true)
      .build();
}
