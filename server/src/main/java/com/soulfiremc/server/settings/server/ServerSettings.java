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
import lombok.RequiredArgsConstructor;

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
  public static final ComboProperty EMAIL_TYPE =
    ImmutableComboProperty.builder()
      .namespace(NAMESPACE)
      .key("email-type")
      .uiName("Email Type")
      .description("How emails should be delivered.")
      .defaultValue(EmailType.CONSOLE.name())
      .addOptions(ComboProperty.optionsFromEnum(EmailType.values(), EmailType::toString))
      .build();
  public static final StringProperty SMTP_HOST =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-host")
      .uiName("SMTP Host")
      .description("SMTP server host to use for sending emails.")
      .defaultValue("smtp.gmail.com")
      .build();
  public static final IntProperty SMTP_PORT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-port")
      .uiName("SMTP Port")
      .description("SMTP server port to use for sending emails.")
      .defaultValue(587)
      .minValue(1)
      .maxValue(65535)
      .thousandSeparator(false)
      .build();
  public static final StringProperty SMTP_USERNAME =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-username")
      .uiName("SMTP Username")
      .description("Username to use for SMTP authentication.")
      .defaultValue("")
      .build();
  public static final StringProperty SMTP_PASSWORD =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-password")
      .uiName("SMTP Password")
      .description("Password to use for SMTP authentication.")
      .defaultValue("")
      .secret(true)
      .build();
  public static final ComboProperty SMTP_TYPE =
    ImmutableComboProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-type")
      .uiName("SMTP Type")
      .description("Type of encryption to use for SMTP.")
      .defaultValue(SmtpType.STARTTLS.name())
      .addOptions(ComboProperty.optionsFromEnum(SmtpType.values(), SmtpType::toString))
      .build();
  public static final StringProperty SMTP_FROM =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("smtp-from")
      .uiName("SMTP From")
      .description("Email address to use as sender for emails.")
      .defaultValue("soulfire@gmail.com")
      .build();

  @RequiredArgsConstructor
  public enum EmailType {
    CONSOLE("Console"),
    SMTP("SMTP");

    private final String uiName;

    @Override
    public String toString() {
      return uiName;
    }
  }

  @RequiredArgsConstructor
  public enum SmtpType {
    STARTTLS("STARTTLS"),
    SSL_TLS("SSL/TLS"),
    NONE("None");

    private final String uiName;

    @Override
    public String toString() {
      return uiName;
    }
  }
}
