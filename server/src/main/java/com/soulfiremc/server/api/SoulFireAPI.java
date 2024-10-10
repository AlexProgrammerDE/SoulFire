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
package com.soulfiremc.server.api;

import com.soulfiremc.server.plugins.*;
import com.soulfiremc.server.util.SFFeatureFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all instances of plugins running in this JVM.
 * The SoulFire server accesses this class on startup and advertises itself to all plugins.
 */
public class SoulFireAPI {
  private static final List<Plugin> SERVER_EXTENSIONS = new ArrayList<>();

  static {
    var plugins =
      new InternalPlugin[]{
        new ClientBrand(),
        new ClientSettings(),
        new ChatControl(),
        new AutoReconnect(),
        new AutoRegister(),
        new AutoRespawn(),
        new AutoTotem(),
        new AutoJump(),
        new AutoChatMessage(),
        new AutoArmor(),
        new AutoEat(),
        new AntiAFK(),
        new ChatMessageLogger(),
        new ServerListBypass(),
        new FakeVirtualHost(), // Needs to be before ModLoaderSupport to not break it
        SFFeatureFlags.MOD_SUPPORT
          ? new ModLoaderSupport()
          : null, // Needs to be before ForwardingBypass to not break it
        new ForwardingBypass(),
        new KillAura(),
        new POVServer()
      };

    for (var plugin : plugins) {
      if (plugin == null) {
        continue;
      }

      SERVER_EXTENSIONS.add(plugin);
    }
  }

  private SoulFireAPI() {}

  public static void registerServerExtension(Plugin plugin) {
    SERVER_EXTENSIONS.add(plugin);
  }

  public static List<Plugin> getServerExtensions() {
    return Collections.unmodifiableList(SERVER_EXTENSIONS);
  }
}
