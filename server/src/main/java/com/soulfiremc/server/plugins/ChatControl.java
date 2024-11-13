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
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

@Extension
public class ChatControl extends InternalPlugin {
  public ChatControl() {
    super(new PluginInfo(
      "chat-control",
      "1.0.0",
      "Control the bot with chat messages",
      "AlexProgrammerDE",
      "GPL-3.0"
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
    private static final Property.Builder BUILDER = Property.builder("chat-control");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Chat Control",
        "Enable controlling the bot with chat messages",
        false);
    public static final StringProperty COMMAND_PREFIX =
      BUILDER.ofString(
        "command-prefix",
        "Command Prefix",
        "Word to put before a command to make the bot execute it",
        "$");
  }
}
