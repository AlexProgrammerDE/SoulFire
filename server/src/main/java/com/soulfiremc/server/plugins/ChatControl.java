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

import com.soulfiremc.brigadier.LocalConsole;
import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

public class ChatControl implements InternalPlugin {
  public static void onChat(ChatMessageReceiveEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(ChatControlSettings.ENABLED)) {
      return;
    }

    var plainMessage = event.parseToText();
    var prefix = settingsHolder.get(ChatControlSettings.COMMAND_PREFIX);

    if (plainMessage.startsWith(prefix)) {
      var command = plainMessage.substring(prefix.length());
      connection.logger().info("[ChatControl] Executing command: {}", command);
      var code = connection.attackManager()
        .soulFireServer()
        .injector()
        .getSingleton(ServerCommandManager.class)
        .execute(command, new LocalConsole());

      connection.botControl().sendMessage("Command \"" + command + "\" executed! (Code: " + code + ")");
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ChatControlSettings.class, "Chat Control");
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(ChatControl.class);
    PluginHelper.registerBotEventConsumer(ChatMessageReceiveEvent.class, ChatControl::onChat);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class ChatControlSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("chat-control");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable controlling the bot with chat messages",
        new String[] {"--chat-control"},
        "Enable controlling the bot with chat messages",
        false);
    public static final StringProperty COMMAND_PREFIX =
      BUILDER.ofString(
        "command-prefix",
        "Command Prefix",
        new String[] {"--command-prefix"},
        "Word to put before a command to make the bot execute it",
        "$");
  }
}
