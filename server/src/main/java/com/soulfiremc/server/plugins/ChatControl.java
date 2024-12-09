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

import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.brigadier.ServerConsoleCommandSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableStringProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class ChatControl extends InternalPlugin {
  public ChatControl() {
    super(new PluginInfo(
      "chat-control",
      "1.0.0",
      "Control the bot with chat messages",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onChat(ChatMessageReceiveEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    if (!settingsSource.get(ChatControlSettings.ENABLED)) {
      return;
    }

    var plainMessage = event.parseToPlainText();
    var prefix = settingsSource.get(ChatControlSettings.COMMAND_PREFIX);
    var prefixIndex = plainMessage.indexOf(prefix);
    if (prefixIndex == -1) {
      return;
    }

    plainMessage = plainMessage.substring(prefixIndex);
    var command = plainMessage.substring(prefix.length());
    connection.logger().info("[ChatControl] Executing command: \"{}\"", command);

    ServerCommandManager.putInstanceIds(List.of(connection.instanceManager().id()));
    var code = connection.instanceManager()
      .soulFireServer()
      .injector()
      .getSingleton(ServerCommandManager.class)
      .execute(command, ServerConsoleCommandSource.INSTANCE);

    connection.botControl().sendMessage("Command \"%s\" executed! (Code: %d)".formatted(command, code));
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ChatControlSettings.class, "Chat Control", this, "joystick");
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class ChatControlSettings implements SettingsObject {
    private static final String NAMESPACE = "chat-control";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Chat Control")
        .description("Enable controlling the bot with chat messages")
        .defaultValue(false)
        .build();
    public static final StringProperty COMMAND_PREFIX =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("command-prefix")
        .uiName("Command Prefix")
        .description("Word to put before a command to make the bot execute it")
        .defaultValue("$")
        .build();
  }
}
