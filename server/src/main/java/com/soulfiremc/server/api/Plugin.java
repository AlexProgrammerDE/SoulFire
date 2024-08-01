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

import com.soulfiremc.server.SoulFireServer;

/**
 * This interface is for any plugin that hooks into the server.
 */
public sealed interface Plugin permits ExternalPlugin, InternalPlugin {
  PluginInfo pluginInfo();

  /**
   * When a new SoulFire server became ready for you to use.
   * Be aware this method may be called multiple times.
   * There is no guarantee that only one SoulFireServer may be created.
   *
   * @param soulFireServer The server instance.
   */
  void onServer(SoulFireServer soulFireServer);
}
