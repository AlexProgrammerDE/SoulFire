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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableStringProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

@Extension
public class AutoRegister extends InternalPlugin {
  public AutoRegister() {
    super(new PluginInfo(
      "auto-register",
      "1.0.0",
      "Automatically registers bots on servers",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onChat(ChatMessageReceiveEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    if (!settingsSource.get(AutoRegisterSettings.ENABLED)) {
      return;
    }

    var plainMessage = event.parseToPlainText();
    var password = settingsSource.get(AutoRegisterSettings.PASSWORD_FORMAT);

    // TODO: Add more password options
    if (plainMessage.contains("/register")) {
      var registerCommand = settingsSource.get(AutoRegisterSettings.REGISTER_COMMAND);
      connection.botControl().sendMessage(registerCommand.replace("%password%", password));
    } else if (plainMessage.contains("/login")) {
      var loginCommand = settingsSource.get(AutoRegisterSettings.LOGIN_COMMAND);
      connection.botControl().sendMessage(loginCommand.replace("%password%", password));
    } else if (plainMessage.contains("/captcha")) {
      var captchaCommand = settingsSource.get(AutoRegisterSettings.CAPTCHA_COMMAND);
      var split = plainMessage.split(" ");

      for (var i = 0; i < split.length; i++) {
        if (split[i].equals("/captcha")) {
          connection.botControl().sendMessage(captchaCommand.replace("%captcha%", split[i + 1]));
        }
      }
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(AutoRegisterSettings.class, "Auto Register", this, "key-round", AutoRegisterSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoRegisterSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-register";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Register")
        .description("Make bots run the /register and /login command after joining")
        .defaultValue(false)
        .build();
    public static final StringProperty REGISTER_COMMAND =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("register-command")
        .uiName("Register Command")
        .description("Command to be executed to register")
        .defaultValue("/register %password% %password%")
        .build();
    public static final StringProperty LOGIN_COMMAND =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("login-command")
        .uiName("Login Command")
        .description("Command to be executed to log in")
        .defaultValue("/login %password%")
        .build();
    public static final StringProperty CAPTCHA_COMMAND =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("captcha-command")
        .uiName("Captcha Command")
        .description("Command to be executed to confirm a captcha")
        .defaultValue("/captcha %captcha%")
        .build();
    public static final StringProperty PASSWORD_FORMAT =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("password-format")
        .uiName("Password Format")
        .description("The password for registering")
        .defaultValue("SoulFire")
        .build();
  }
}
